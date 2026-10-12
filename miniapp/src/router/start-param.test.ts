import { describe, expect, it } from 'vitest';
import { deepLinkPath, resolveStartParam } from './start-param';

const SAMPLE_UUID = '019602a3-7b2c-7000-8000-000000000001';

/**
 * آزمون مسیریابی Deep Link.
 *
 * <p>⚠️ این آزمون‌ها <b>امنیتی</b> هستند، نه عملکردی: {@code start_param}
 * تنها ورودی‌ای است که هر کاربری می‌تواند آزادانه بسازد و برای معلم
 * بفرستد. پس تمرکز روی <i>رد کردن</i> است، نه پذیرفتن.
 */
describe('resolveStartParam', () => {
  it('سه پیشوند مستندشده را با شناسه عددی می‌پذیرد', () => {
    expect(resolveStartParam('asg_123')).toEqual({ kind: 'assignment', id: '123' });
    expect(resolveStartParam('cls_45')).toEqual({ kind: 'class', id: '45' });
    expect(resolveStartParam('exm_7')).toEqual({ kind: 'exam', id: '7' });
  });

  it('سه پیشوند مستندشده را با UUID می‌پذیرد', () => {
    expect(resolveStartParam(`asg_${SAMPLE_UUID}`)).toEqual({
      kind: 'assignment',
      id: SAMPLE_UUID,
    });
    expect(resolveStartParam(`cls_${SAMPLE_UUID}`)).toEqual({
      kind: 'class',
      id: SAMPLE_UUID,
    });
    expect(resolveStartParam(`exm_${SAMPLE_UUID}`)).toEqual({
      kind: 'exam',
      id: SAMPLE_UUID,
    });
  });

  it('UUID با حروف بزرگ را می‌پذیرد', () => {
    const upper = '019602A3-7B2C-7000-8000-000000000001';
    expect(resolveStartParam(`asg_${upper}`)).toEqual({ kind: 'assignment', id: upper });
  });

  it('شناسه ۱۹ رقمی (سقف long) را می‌پذیرد', () => {
    const nineteen = '9'.repeat(19);

    expect(resolveStartParam(`asg_${nineteen}`)).toEqual({
      kind: 'assignment',
      id: nineteen,
    });
  });

  it('شناسه ۲۰ رقمی را رد می‌کند — سقف دفاعی', () => {
    expect(resolveStartParam(`asg_${'9'.repeat(20)}`)).toBeNull();
  });

  it.each([
    [`asg_${SAMPLE_UUID}-extra`, 'UUID با پسوند اضافه'],
    [`asg_${SAMPLE_UUID.slice(0, 35)}`, 'UUID ناقص'],
    [`asg_${SAMPLE_UUID.replace(/-/g, '')}`, 'UUID بدون خط تیره'],
  ])('%s را رد می‌کند (%s)', (input) => {
    expect(resolveStartParam(input)).toBeNull();
  });

  it.each([
    // 🔴 هر یک از این‌ها یک راه حمله بود که اگر «تقریباً» می‌پذیرفتیم،
    // به یک مسیر ساختگی می‌رسید.
    ['asg_1_2', 'دوبخش و بیشتر'],
    ['asg_1&cls_2', 'چسباندن دو مقدار'],
    ['asg_', 'شناسه خالی'],
    ['_123', 'پیشوند خالی'],
    ['xyz_123', 'پیشوند ناشناخته'],
    ['ASG_123', 'حروف بزرگ'],
    ['asg_12a', 'شناسه غیرعددی'],
    ['asg_-12', 'شناسه منفی'],
    ['asg_1e5', 'نمایش علمی — از قلم افتاده‌ترین مورد'],
    ['asg_ 12 ', 'فاصله اضافه'],
    ['asg_0x1f', 'مبنای شانزده'],
    ['asg_Infinity', 'عدد نامتناهی'],
    ['asg_12.5', 'اعشار'],
    ['asg_१२', 'رقم غیر ASCII (هندی)'],
    ['asg_１２', 'رقم تمام‌عرض'],
    ['asg_12\n', 'کاراکتر کنترلی'],
    ['../../etc/passwd', 'تلاش پیمایش مسیر'],
    ['asg_1?x=2', 'دستکاری کوئری'],
    ['asg_1#frag', 'Fragment'],
    ['', 'رشته خالی'],
    ['asg', 'بدون جداکننده'],
    ['_', 'فقط جداکننده'],
  ])('%s را رد می‌کند (%s)', (input) => {
    expect(resolveStartParam(input)).toBeNull();
  });

  it.each([undefined, null])('ورودی %s را رد می‌کند بدون پرتاب', (input) => {
    expect(resolveStartParam(input)).toBeNull();
  });

  it('مقدار خیلی طولانی را سریع رد می‌کند', () => {
    // ⚠️ اگر این «کند» بود یعنی کسی روی کل رشته regex اجرا می‌کند.
    expect(resolveStartParam(`asg_${'1'.repeat(100_000)}`)).toBeNull();
  });
});

describe('deepLinkPath', () => {
  it('برای هر نوع یک مسیر ثابت می‌سازد', () => {
    expect(deepLinkPath({ kind: 'assignment', id: '1' })).toBe('/assignments/1');
    expect(deepLinkPath({ kind: 'class', id: '2' })).toBe('/classes/2');
    expect(deepLinkPath({ kind: 'exam', id: '3' })).toBe('/exams/3/take');
  });

  it('UUID را بدون دستکاری در مسیر قرار می‌دهد', () => {
    expect(deepLinkPath({ kind: 'assignment', id: SAMPLE_UUID })).toBe(
      `/assignments/${SAMPLE_UUID}`,
    );
  });

  it('هیچ‌وقت روی مقدار خام کار نمی‌کند — فقط شناسه معتبر عبور می‌کند', () => {
    // ⚠️ این آزمون قرارداد است: اگر روزی کسی مسیر را با رشته خام ساخت،
    // همان `../` معروف به مسیر راه پیدا می‌کند.
    const target = resolveStartParam(`asg_${SAMPLE_UUID}`);

    expect(target).not.toBeNull();
    expect(deepLinkPath(target!)).not.toContain('..');
    expect(deepLinkPath(target!)).toBe(`/assignments/${SAMPLE_UUID}`);
  });
});

package ir.omid.smartteacher.book.internal.api.mapper;

import ir.omid.smartteacher.book.internal.api.dto.BookPageResponse;
import ir.omid.smartteacher.book.internal.api.dto.BookResponse;
import ir.omid.smartteacher.book.internal.api.dto.ChapterResponse;
import ir.omid.smartteacher.book.internal.api.dto.ContentAnalysisResponse;
import ir.omid.smartteacher.book.internal.domain.Book;
import ir.omid.smartteacher.book.internal.domain.BookChapter;
import ir.omid.smartteacher.book.internal.domain.BookPage;
import ir.omid.smartteacher.book.internal.domain.ContentAnalysis;
import org.mapstruct.Mapper;

/**
 * نگاشت موجودیت‌های کتاب به DTO.
 */
@Mapper(componentModel = "spring")
public interface BookMapper {

    BookResponse toResponse(Book book);

    BookPageResponse toPageResponse(BookPage page);

    ChapterResponse toChapterResponse(BookChapter chapter);

    ContentAnalysisResponse toAnalysisResponse(ContentAnalysis analysis);
}

package bg.nbu.banksystem.employee.web;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable paging envelope returned by Employee Service list endpoints.
 *
 * @param content       items contained in the current page
 * @param page          zero-based page number
 * @param size          number of items requested per page
 * @param totalElements total number of matching items
 * @param totalPages    total number of available pages
 * @param last          whether this is the final page
 * @param <T>           type of item contained in the page
 */
public record PagedResponse<T>(

        @Schema(description = "Items contained in the current page.")
        List<T> content,

        @Schema(description = "Zero-based page number.", example = "0")
        int page,

        @Schema(description = "Number of items requested per page.", example = "20")
        int size,

        @Schema(description = "Total number of matching items.", example = "42")
        long totalElements,

        @Schema(description = "Total number of available pages.", example = "3")
        int totalPages,

        @Schema(description = "Whether this is the final page.", example = "false")
        boolean last
) {

    /**
     * Converts a Spring Data page into the stable API paging representation.
     *
     * @param source the Spring Data page to convert
     * @param <T>    type of item contained in the page
     * @return the API paging representation
     */
    public static <T> PagedResponse<T> from(Page<T> source) {
        return new PagedResponse<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isLast()
        );
    }
}
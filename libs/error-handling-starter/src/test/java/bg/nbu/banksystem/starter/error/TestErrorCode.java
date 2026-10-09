package bg.nbu.banksystem.starter.error;

import org.jspecify.annotations.NullMarked;

@NullMarked
enum TestErrorCode implements ErrorCode {

    WIDGET_NOT_FOUND(ErrorCategory.NOT_FOUND, "Widget not found");

    private final ErrorCategory category;
    private final String title;

    TestErrorCode(ErrorCategory category, String title) {
        this.category = category;
        this.title = title;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public ErrorCategory category() {
        return category;
    }

    @Override
    public String title() {
        return title;
    }
}
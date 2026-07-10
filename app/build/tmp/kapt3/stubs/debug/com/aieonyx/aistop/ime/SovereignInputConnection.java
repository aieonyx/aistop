package com.aieonyx.aistop.ime;

/**
 * M2 SovereignInputConnection — v1.1 critical fix (Gemini audit).
 *
 * commitText() alone is insufficient. WebViews (ChatGPT web), Flutter wrappers,
 * and custom EditText implementations bypass commitText() and use:
 *  - setComposingText()
 *  - replaceText()
 *  - direct InputConnection manipulation
 *
 * This wrapper intercepts ALL THREE paste delivery paths.
 * pii_detect() is called BEFORE any super() call commits text.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\r\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0018\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\u0016J2\u0010\u000f\u001a\u00020\n2\u0006\u0010\u0010\u001a\u00020\u000e2\u0006\u0010\u0011\u001a\u00020\u000e2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000e2\b\u0010\u0012\u001a\u0004\u0018\u00010\u0013H\u0016J\u0018\u0010\u0014\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\u0016R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0015"}, d2 = {"Lcom/aieonyx/aistop/ime/SovereignInputConnection;", "Landroid/view/inputmethod/InputConnectionWrapper;", "base", "Landroid/view/inputmethod/InputConnection;", "mediator", "Lcom/aieonyx/aistop/ime/PasteMediator;", "targetPackage", "", "(Landroid/view/inputmethod/InputConnection;Lcom/aieonyx/aistop/ime/PasteMediator;Ljava/lang/String;)V", "commitText", "", "text", "", "newCursorPosition", "", "replaceText", "start", "end", "textAttribute", "Landroid/view/inputmethod/TextAttribute;", "setComposingText", "app_debug"})
public final class SovereignInputConnection extends android.view.inputmethod.InputConnectionWrapper {
    @org.jetbrains.annotations.NotNull()
    private final com.aieonyx.aistop.ime.PasteMediator mediator = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String targetPackage = null;
    
    public SovereignInputConnection(@org.jetbrains.annotations.NotNull()
    android.view.inputmethod.InputConnection base, @org.jetbrains.annotations.NotNull()
    com.aieonyx.aistop.ime.PasteMediator mediator, @org.jetbrains.annotations.NotNull()
    java.lang.String targetPackage) {
        super(null, false);
    }
    
    @java.lang.Override()
    public boolean commitText(@org.jetbrains.annotations.NotNull()
    java.lang.CharSequence text, int newCursorPosition) {
        return false;
    }
    
    @java.lang.Override()
    public boolean setComposingText(@org.jetbrains.annotations.NotNull()
    java.lang.CharSequence text, int newCursorPosition) {
        return false;
    }
    
    @java.lang.Override()
    public boolean replaceText(int start, int end, @org.jetbrains.annotations.NotNull()
    java.lang.CharSequence text, int newCursorPosition, @org.jetbrains.annotations.Nullable()
    android.view.inputmethod.TextAttribute textAttribute) {
        return false;
    }
}
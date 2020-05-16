fun View.setGone() {
    this.visibility = View.GONE
}

inline fun <T : View> T.afterMeasured(crossinline f: T.() -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            f()
            viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    })
}

fun String.styleSpan(bold: Boolean? = null, color: Int? = null): SpannableString {
    val span = SpannableString(this)
    if (bold.notNullOrFalse()) span.setSpan(StyleSpan(Typeface.BOLD), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    color?.let { span.setSpan(ForegroundColorSpan(it), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }
    return span
}

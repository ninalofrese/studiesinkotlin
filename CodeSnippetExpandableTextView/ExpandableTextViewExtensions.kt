import android.animation.ObjectAnimator
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Created by Nina Lofrese @ninalofrese on 10/05/20.
 */
// This method not only creates the Expandable, as it hides the entire view or group if the text is null or blank, 
// and place a label before the text
fun TextView.setAsExpandable(text: String?, view: View? = null, maxLines: Int, label: String = "") {
    val labelColor = ContextCompat.getColor(context, R.color.black_2d2d2d)
    if (text.isNullOrBlank()) {
        if (view == null) this.setGone() else view.setGone()
    } else {
        //runs assynchronously and make sure the code that measures is running after first measure
        post {
            this.afterMeasured {
                setExpandText(label, text, maxLines, labelColor)
                setExpandAction(label, text, labelColor)
            }
        }
    }
}

private fun TextView.setExpandAction(label: String = "", text: String, labelColor: Int? = null) {
    //only expands on click (once)
    setOnClickListener {
        val animation: ObjectAnimator = ObjectAnimator.ofInt(this, "maxLines", 30)
        this.text = getFullText(label, text, labelColor)
        animation.setDuration(200L).start()
    }
}

private fun TextView.setExpandText(labelName: String = "", text: String, collapseLines: Int, labelColor: Int?) {
    //avoid Exception for XMLs attributes
    this.maxLines = collapseLines.takeUnless { it <= 0 } ?: 1
    this.ellipsize = null
    
    //the text to be measured is one with the labelText too
    val allText = getFullText(labelName, text, labelColor)
    this.text = allText

    val builder = SpannableStringBuilder()

    try {
        if (lineCount > collapseLines) {
            val lastCharShown = layout.getLineVisibleEnd(maxLines - 1)
            this.maxLines = collapseLines
            val moreString = " ${context.getString(R.string.read_more)}"

            if (labelName.isNotBlank()) builder.append("$labelName  ".styleSpan(bold = true, color = labelColor))
            builder.append(text.substring(0, lastCharShown - moreString.length - 7 - labelName.length))
            builder.append(moreString.styleSpan(color = labelColor))
            this.text = builder
        }
    } catch (e: IndexOutOfBoundsException) {
        //if something happens and launch an Exception, use the "default" ellipsis to the text
        Log.w("ExpandableTextView", "Could not add readMore suffix. Using default ellipsis instead. ${e.cause}")
        this.maxLines = collapseLines
        this.ellipsize = TextUtils.TruncateAt.END
    }
}

private fun getFullText(label: String, text: String, labelColor: Int?): SpannableStringBuilder {
    val builder = SpannableStringBuilder()
    if (label.isNotBlank()) builder.append("$label  ".styleSpan(bold = true, color = labelColor))
    builder.append(text)
    return builder
}

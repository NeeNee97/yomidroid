package com.yomidroid.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Small floating editor for correcting OCR text without leaving the page being read.
 * It is a normal (focusable) application-overlay window, so the keyboard works in it,
 * unlike the accessibility-overlay windows used for the rest of the OCR overlay.
 * Shows a crop of the screen around the text for reference.
 */
class OcrEditWindow(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var root: View? = null

    fun show(initialText: String, crop: Bitmap?, onDone: (String) -> Unit, onCancel: () -> Unit) {
        dismiss()

        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val card = object : LinearLayout(context) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.action == KeyEvent.ACTION_UP) {
                        dismiss()
                        onCancel()
                    }
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(14), dp(12), dp(14), dp(12))
        card.background = GradientDrawable().apply {
            setColor(0xF2202124.toInt())
            cornerRadius = dp(14).toFloat()
        }

        val title = TextView(context)
        title.text = "Edit text"
        title.setTextColor(0xFFAAAAAA.toInt())
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        card.addView(title)

        if (crop != null) {
            val image = ImageView(context)
            image.setImageBitmap(crop)
            image.scaleType = ImageView.ScaleType.FIT_CENTER
            image.setBackgroundColor(Color.BLACK)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(120))
            lp.topMargin = dp(8)
            card.addView(image, lp)
        }

        val edit = EditText(context)
        edit.setText(initialText)
        edit.setSelection(initialText.length)
        edit.setTextColor(Color.WHITE)
        edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        edit.setSingleLine(true)
        edit.imeOptions = EditorInfo.IME_ACTION_DONE
        val editLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        editLp.topMargin = dp(8)
        card.addView(edit, editLp)

        fun submit() {
            val text = edit.text.toString()
            dismiss()
            onDone(text)
        }

        edit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }

        val buttons = LinearLayout(context)
        buttons.orientation = LinearLayout.HORIZONTAL
        buttons.gravity = Gravity.END

        val cancel = Button(context)
        cancel.text = "Cancel"
        cancel.setOnClickListener {
            dismiss()
            onCancel()
        }
        val ok = Button(context)
        ok.text = "Done"
        ok.setOnClickListener { submit() }
        buttons.addView(cancel)
        buttons.addView(ok)
        card.addView(buttons)

        val frame = FrameLayout(context)
        frame.setPadding(dp(8), 0, dp(8), 0)
        frame.addView(card, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP
        params.y = dp(48)
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN or
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE

        try {
            windowManager.addView(frame, params)
            root = frame
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show OCR edit window: ${e.message}")
            onCancel()
            return
        }

        edit.requestFocus()
        edit.post {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun dismiss() {
        val v = root ?: return
        root = null
        try {
            windowManager.removeView(v)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to remove OCR edit window: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "OcrEditWindow"
    }
}

package com.mooserella.thairandomizer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View

/** A small field where you draw a letter with your finger. */
class DrawPad(ctx: Context, private val bgColor: Int) : View(ctx) {
    private val strokes = ArrayList<Path>()
    private var current: Path? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 7f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onMeasure(w: Int, h: Int) {
        val width = MeasureSpec.getSize(w)
        val height = if (MeasureSpec.getMode(h) == MeasureSpec.UNSPECIFIED) minimumHeight
        else maxOf(MeasureSpec.getSize(h), minimumHeight)
        setMeasuredDimension(width, height)
    }

    fun clear() {
        strokes.clear()
        current = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(bgColor)
        for (p in strokes) canvas.drawPath(p, paint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val p = Path().apply { moveTo(e.x, e.y); lineTo(e.x + 0.1f, e.y) }
                strokes.add(p)
                current = p
            }
            MotionEvent.ACTION_MOVE -> {
                val p = current ?: return true
                for (i in 0 until e.historySize) p.lineTo(e.getHistoricalX(i), e.getHistoricalY(i))
                p.lineTo(e.x, e.y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> current = null
        }
        invalidate()
        return true
    }
}

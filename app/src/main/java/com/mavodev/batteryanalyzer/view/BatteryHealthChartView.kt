package com.mavodev.batteryanalyzer.view

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.mavodev.batteryanalyzer.R
import com.mavodev.batteryanalyzer.model.HistoryEntry
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

class BatteryHealthChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var entries: List<HistoryEntry> = emptyList()
    
    private val isDarkMode: Boolean
        get() = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    
    private val textColor: Int
        get() = if (isDarkMode) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
    
    private val axisColor: Int
        get() = if (isDarkMode) 0x80FFFFFF.toInt() else 0x60000000.toInt()
    
    private val gridColor: Int
        get() = if (isDarkMode) 0x30FFFFFF.toInt() else 0x30000000.toInt()
    
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = ContextCompat.getColor(context, R.color.health_good)
    }
    
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.health_good)
        alpha = 40
    }
    
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.health_good)
    }
    
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }
    
    // Paints for Calculated Health
    private val calculatedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = 0xFF9E9E9E.toInt() // Gray
    }
    
    private val calculatedPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF9E9E9E.toInt() // Gray
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
    }
    
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 36f
        isFakeBoldText = true
    }
    
    private val leftPadding = 100f
    private val rightPadding = 40f
    private val topPadding = 80f
    private val bottomPadding = 80f
    
    fun setData(historyEntries: List<HistoryEntry>) {
        // Filter entries with valid health percentage and sort by timestamp
        entries = historyEntries
            .filter { it.healthPercentage != null || it.calculatedHealthPercentage != null }
            .sortedBy { it.timestamp }
        
        // Update colors based on latest health
        if (entries.isNotEmpty()) {
            val latestHealth = entries.last().healthPercentage ?: 100.0
            val color = getHealthColor(latestHealth)
            linePaint.color = color
            pointPaint.color = color
            fillPaint.color = color
            fillPaint.alpha = 40
        }
        
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Update colors based on current theme
        textPaint.color = textColor
        titlePaint.color = textColor
        gridPaint.color = gridColor
        
        if (entries.isEmpty()) {
            drawEmptyState(canvas)
            return
        }
        
        if (entries.size == 1) {
            drawSinglePoint(canvas)
            return
        }
        
        drawChart(canvas)
        drawLegend(canvas)
    }
    
    private fun drawEmptyState(canvas: Canvas) {
        val text = "No health data available"
        val textWidth = textPaint.measureText(text)
        canvas.drawText(
            text,
            (width - textWidth) / 2,
            height / 2f,
            textPaint
        )
    }
    
    private fun drawSinglePoint(canvas: Canvas) {
        val entry = entries.first()
        val health = entry.healthPercentage ?: return
        
        // Draw title
        val title = "Battery Health History"
        val titleWidth = titlePaint.measureText(title)
        canvas.drawText(title, (width - titleWidth) / 2, 50f, titlePaint)
        
        // Draw single point centered
        val centerX = width / 2f
        val chartHeight = height - topPadding - bottomPadding
        val y = topPadding + chartHeight * (1 - (health - 60) / 40).toFloat()
        
        canvas.drawCircle(centerX, y, 8f, pointPaint)
        
        // Draw health value
        val healthText = String.format("%.1f%%", health)
        val textWidth = textPaint.measureText(healthText)
        canvas.drawText(healthText, centerX - textWidth / 2, y - 15f, textPaint)
    }
    
    private fun drawChart(canvas: Canvas) {
        val chartWidth = width - leftPadding - rightPadding
        val chartHeight = height - topPadding - bottomPadding
        
        // Draw title
        val title = "Battery Health History"
        val titleWidth = titlePaint.measureText(title)
        canvas.drawText(title, (width - titleWidth) / 2, 50f, titlePaint)
        
        // Calculate min/max for better scaling
        val healthValues = entries.mapNotNull { it.healthPercentage } + 
                          entries.mapNotNull { it.calculatedHealthPercentage }
        if (healthValues.isEmpty()) return
        
        val minHealth = max(60.0, healthValues.minOrNull()!! - 5)
        val maxHealth = min(110.0, healthValues.maxOrNull()!! + 5)
        val healthRange = maxHealth - minHealth
        
        if (healthRange < 0.1) return
        
        // Draw Y-axis
        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = axisColor
        }
        canvas.drawLine(leftPadding, topPadding, leftPadding, height - bottomPadding, axisPaint)
        
        // Draw X-axis
        canvas.drawLine(leftPadding, height - bottomPadding, width - rightPadding, height - bottomPadding, axisPaint)
        
        // Draw Y-axis label
        val yAxisLabel = "Health %"
        canvas.save()
        canvas.rotate(-90f, 30f, height / 2f)
        canvas.drawText(yAxisLabel, 30f, height / 2f, textPaint)
        canvas.restore()
        
        // Draw grid lines and Y-axis labels
        for (i in 0..4) {
            val y = topPadding + (chartHeight * i / 4)
            canvas.drawLine(leftPadding, y, width - rightPadding, y, gridPaint)
            
            // Draw health labels
            val healthValue = maxHealth - (healthRange * i / 4)
            val label = String.format("%.0f%%", healthValue)
            val labelWidth = textPaint.measureText(label)
            canvas.drawText(label, leftPadding - labelWidth - 15f, y + 8f, textPaint)
        }
        
        // Create path for line and fill (Reported)
        val linePath = Path()
        val fillPath = Path()
        val reportedPoints = mutableListOf<Pair<Float, Float>>()

        // Create path for Calculated line
        val calculatedLinePath = Path()
        val calculatedPoints = mutableListOf<Pair<Float, Float>>()
        
        entries.forEachIndexed { index, entry ->
            val x = leftPadding + (chartWidth * index / (entries.size - 1))
            
            // Reported Point
            entry.healthPercentage?.let { health ->
                val y = topPadding + chartHeight * (1 - (health - minHealth) / healthRange)
                reportedPoints.add(Pair(x, y.toFloat()))
                
                if (reportedPoints.size == 1) {
                    linePath.moveTo(x, y.toFloat())
                    fillPath.moveTo(x, height - bottomPadding)
                    fillPath.lineTo(x, y.toFloat())
                } else {
                    linePath.lineTo(x, y.toFloat())
                    fillPath.lineTo(x, y.toFloat())
                }
            }

            // Calculated Point
            entry.calculatedHealthPercentage?.let { health ->
                val y = topPadding + chartHeight * (1 - (health - minHealth) / healthRange)
                calculatedPoints.add(Pair(x, y.toFloat()))
                
                if (calculatedPoints.size == 1) {
                    calculatedLinePath.moveTo(x, y.toFloat())
                } else {
                    calculatedLinePath.lineTo(x, y.toFloat())
                }
            }
        }
        
        // Draw fill (Reported)
        if (reportedPoints.isNotEmpty()) {
            fillPath.lineTo(reportedPoints.last().first, height - bottomPadding)
            fillPath.close()
            canvas.drawPath(fillPath, fillPaint)
        }
        
        // Draw Calculated line
        canvas.drawPath(calculatedLinePath, calculatedLinePaint)
        
        // Draw Reported line
        canvas.drawPath(linePath, linePaint)
        
        // Draw points
        reportedPoints.forEach { (x, y) ->
            canvas.drawCircle(x, y, 6f, pointPaint)
        }
        calculatedPoints.forEach { (x, y) ->
            canvas.drawCircle(x, y, 4f, calculatedPointPaint)
        }
        
        // Draw X-axis labels (dates)
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        
        val firstDate = dateFormat.format(Date(entries.first().timestamp))
        canvas.drawText(firstDate, leftPadding, height - bottomPadding + 30f, textPaint)
        
        val lastDate = dateFormat.format(Date(entries.last().timestamp))
        val lastDateWidth = textPaint.measureText(lastDate)
        canvas.drawText(lastDate, width - rightPadding - lastDateWidth, height - bottomPadding + 30f, textPaint)
        
        // Draw X-axis label
        val xAxisLabel = "Timeline"
        val xLabelWidth = textPaint.measureText(xAxisLabel)
        canvas.drawText(xAxisLabel, (width - xLabelWidth) / 2, height - 15f, textPaint)
        
        // Show trend indicator
        if (entries.size >= 2) {
            val firstHealth = entries.first().healthPercentage ?: return
            val lastHealth = entries.last().healthPercentage ?: return
            val change = lastHealth - firstHealth
            
            val trendText = if (change >= 0) {
                String.format("▲ +%.1f%%", change)
            } else {
                String.format("▼ %.1f%%", change)
            }
            
            val trendPaint = Paint(textPaint).apply {
                color = if (change >= 0) ContextCompat.getColor(context, R.color.health_good) 
                       else ContextCompat.getColor(context, R.color.health_poor)
                textSize = 32f
                isFakeBoldText = true
            }
            
            val trendWidth = trendPaint.measureText(trendText)
            canvas.drawText(trendText, width - rightPadding - trendWidth, 50f, trendPaint)
        }
    }
    
    private fun drawLegend(canvas: Canvas) {
        val legendY = 85f
        val legendItemWidth = 160f
        var currentX = leftPadding
        
        // Reported Legend
        val reportedLabel = "Reported"
        canvas.drawCircle(currentX + 10f, legendY - 10f, 6f, pointPaint)
        canvas.drawText(reportedLabel, currentX + 25f, legendY, textPaint)
        
        currentX += legendItemWidth
        
        // Calculated Legend (if data exists)
        if (entries.any { it.calculatedHealthPercentage != null }) {
            val calculatedLabel = "Calculated"
            canvas.drawCircle(currentX + 10f, legendY - 10f, 5f, calculatedPointPaint)
            canvas.drawText(calculatedLabel, currentX + 25f, legendY, textPaint)
        }
    }
    
    private fun getHealthColor(healthPercentage: Double): Int {
        return when {
            healthPercentage >= 95 -> ContextCompat.getColor(context, R.color.health_excellent)
            healthPercentage >= 85 -> ContextCompat.getColor(context, R.color.health_good)
            healthPercentage >= 75 -> ContextCompat.getColor(context, R.color.health_fair)
            healthPercentage >= 65 -> ContextCompat.getColor(context, R.color.health_poor)
            else -> ContextCompat.getColor(context, R.color.health_critical)
        }
    }
}

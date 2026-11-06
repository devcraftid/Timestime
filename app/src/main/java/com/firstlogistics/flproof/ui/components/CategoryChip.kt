package com.firstlogistics.flproof.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstlogistics.flproof.data.model.DocumentationCategory
import com.firstlogistics.flproof.ui.theme.*

@Composable
fun CategoryChip(
    category: DocumentationCategory,
    isSelected: Boolean,
    onSelected: (DocumentationCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) GreenAccent else NavyCard
    val textColor = if (isSelected) PureWhite else TextWhite
    val border = if (isSelected) null else BorderStroke(1.dp, NavyBorder)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = border,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelected(category) }
    ) {
        Text(
            text = category.title,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}


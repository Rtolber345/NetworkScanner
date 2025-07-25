package com.example.networkscanner.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TypographyExample(
    val name: String,
    val description: String,
    val textStyle: androidx.compose.ui.text.TextStyle,
    val usage: String
)

@Composable
fun Material3TypographyChart(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val typographyExamples = listOf(
        TypographyExample(
            name = "Display Large",
            description = "57sp • For the largest text on screen",
            textStyle = MaterialTheme.typography.displayLarge,
            usage = "Hero headlines, brand names"
        ),
            TypographyExample(
                name = "Display Medium", 
                description = "45sp • Short, important text",
                textStyle = MaterialTheme.typography.displayMedium,
                usage = "Section headers, feature callouts"
            ),
            TypographyExample(
                name = "Display Small",
                description = "36sp • Headlines in smaller spaces", 
                textStyle = MaterialTheme.typography.displaySmall,
                usage = "Card headers, dialog titles"
            ),
            TypographyExample(
                name = "Headline Large",
                description = "32sp • High-emphasis text",
                textStyle = MaterialTheme.typography.headlineLarge,
                usage = "Page titles, important announcements"
            ),
            TypographyExample(
                name = "Headline Medium",
                description = "28sp • Moderately high emphasis",
                textStyle = MaterialTheme.typography.headlineMedium,
                usage = "Section titles, card headers"
            ),
            TypographyExample(
                name = "Headline Small",
                description = "24sp • Slightly high emphasis",
                textStyle = MaterialTheme.typography.headlineSmall,
                usage = "Sub-headers, list headers"
            ),
            TypographyExample(
                name = "Title Large",
                description = "22sp • Medium emphasis",
                textStyle = MaterialTheme.typography.titleLarge,
                usage = "Toolbar titles, tab labels"
            ),
            TypographyExample(
                name = "Title Medium",
                description = "16sp • Medium emphasis",
                textStyle = MaterialTheme.typography.titleMedium,
                usage = "Card titles, list items"
            ),
            TypographyExample(
                name = "Title Small",
                description = "14sp • Medium emphasis", 
                textStyle = MaterialTheme.typography.titleSmall,
                usage = "Dense lists, captions"
            ),
            TypographyExample(
                name = "Body Large",
                description = "16sp • Primary body text",
                textStyle = MaterialTheme.typography.bodyLarge,
                usage = "Articles, descriptions, content"
            ),
            TypographyExample(
                name = "Body Medium",
                description = "14sp • Default body text",
                textStyle = MaterialTheme.typography.bodyMedium,
                usage = "Most readable text, paragraphs"
            ),
            TypographyExample(
                name = "Body Small",
                description = "12sp • Supporting text",
                textStyle = MaterialTheme.typography.bodySmall,
                usage = "Captions, helper text, metadata"
            ),
            TypographyExample(
                name = "Label Large",
                description = "14sp • Call-to-action text",
                textStyle = MaterialTheme.typography.labelLarge,
                usage = "Button text, prominent labels"
            ),
            TypographyExample(
                name = "Label Medium",
                description = "12sp • Standard labels",
                textStyle = MaterialTheme.typography.labelMedium,
                usage = "Form labels, menu items"
            ),
            TypographyExample(
                name = "Label Small",
                description = "11sp • Small labels",
                textStyle = MaterialTheme.typography.labelSmall,
                usage = "Badges, chips, timestamps"
            )
        )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Material 3 Typography",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Expressive type scale system",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Typography examples
            LazyColumn(
                modifier = Modifier.heightIn(max = 600.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(typographyExamples) { example ->
                    TypographyExampleCard(example)
                }
            }
        }
    }
}

@Composable
private fun TypographyExampleCard(
    example: TypographyExample,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Type name and description
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = example.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = example.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = if (example.textStyle.fontSize.isSp) "${example.textStyle.fontSize.value.toInt()}sp" else "?sp",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Example text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                Text(
                    text = "Network Scanner Pro",
                    style = example.textStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Usage description
            Text(
                text = "Usage: ${example.usage}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
fun AnimatedTypographyShowcase(
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val examples = listOf(
        "Display Large" to MaterialTheme.typography.displayLarge,
        "Headline Medium" to MaterialTheme.typography.headlineMedium,
        "Title Large" to MaterialTheme.typography.titleLarge,
        "Body Large" to MaterialTheme.typography.bodyLarge,
        "Label Medium" to MaterialTheme.typography.labelMedium
    )
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3000)
            currentIndex = (currentIndex + 1) % examples.size
        }
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = examples[currentIndex],
                transitionSpec = {
                    fadeIn(animationSpec = androidx.compose.animation.core.tween(1000)) togetherWith
                    fadeOut(animationSpec = androidx.compose.animation.core.tween(1000))
                },
                label = "typography_animation"
            ) { (name, style) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Network Scanner Pro",
                        style = style,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
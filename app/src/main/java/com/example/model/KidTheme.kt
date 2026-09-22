package com.example.model

import androidx.compose.ui.graphics.Color

data class KidTheme(
    val id: String,
    val nameRes: Int,
    val emoji: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val mascotEmoji: String,
    val tagline: String,
    val particleType: ParticleType
)

enum class ParticleType {
    STARS,
    METEORS,
    GAMING_BLOCKS,
    DINO_FOOTPRINTS,
    PAW_PRINTS,
    SPEED_LINES,
    SOCCER_BALLS,
    MAGIC_SPARKLES,
    BUBBLES,
    ROBOT_GEARS,
    RAINBOW_CIRCLES,
    JUNGLE_LEAVES
}

object ThemeRegistry {
    val themes = listOf(
        KidTheme(
            id = "space",
            nameRes = com.example.R.string.theme_space,
            emoji = "🌌",
            primaryColor = Color(0xFF6C63FF),
            secondaryColor = Color(0xFF4834DF),
            accentColor = Color(0xFFFFD166),
            backgroundColor = Color(0xFF0F0E26),
            surfaceColor = Color(0xFF1E1B4B),
            mascotEmoji = "👩‍🚀",
            tagline = "Blast off into deep space exploration!",
            particleType = ParticleType.STARS
        ),
        KidTheme(
            id = "galaxy",
            nameRes = com.example.R.string.theme_galaxy,
            emoji = "🚀",
            primaryColor = Color(0xFF8B5CF6),
            secondaryColor = Color(0xFF6D28D9),
            accentColor = Color(0xFFF472B6),
            backgroundColor = Color(0xFF130924),
            surfaceColor = Color(0xFF241442),
            mascotEmoji = "🚀",
            tagline = "Cosmic adventures among the planets!",
            particleType = ParticleType.METEORS
        ),
        KidTheme(
            id = "gaming",
            nameRes = com.example.R.string.theme_gaming,
            emoji = "🎮",
            primaryColor = Color(0xFF10B981),
            secondaryColor = Color(0xFF047857),
            accentColor = Color(0xFFFBBF24),
            backgroundColor = Color(0xFF0F172A),
            surfaceColor = Color(0xFF1E293B),
            mascotEmoji = "👾",
            tagline = "Press Start for legendary fun!",
            particleType = ParticleType.GAMING_BLOCKS
        ),
        KidTheme(
            id = "dinosaurs",
            nameRes = com.example.R.string.theme_dinosaurs,
            emoji = "🦖",
            primaryColor = Color(0xFF22C55E),
            secondaryColor = Color(0xFF15803D),
            accentColor = Color(0xFFEAB308),
            backgroundColor = Color(0xFF142417),
            surfaceColor = Color(0xFF213B26),
            mascotEmoji = "🦕",
            tagline = "Roar with friendly prehistoric giants!",
            particleType = ParticleType.DINO_FOOTPRINTS
        ),
        KidTheme(
            id = "animals",
            nameRes = com.example.R.string.theme_animals,
            emoji = "🐼",
            primaryColor = Color(0xFFF43F5E),
            secondaryColor = Color(0xFFBE123C),
            accentColor = Color(0xFFFED7AA),
            backgroundColor = Color(0xFF271318),
            surfaceColor = Color(0xFF3F1D26),
            mascotEmoji = "🐼",
            tagline = "Cuddle with cute and fluffy animal pals!",
            particleType = ParticleType.PAW_PRINTS
        ),
        KidTheme(
            id = "racing",
            nameRes = com.example.R.string.theme_racing,
            emoji = "🏎",
            primaryColor = Color(0xFFEF4444),
            secondaryColor = Color(0xFFB91C1C),
            accentColor = Color(0xFFF59E0B),
            backgroundColor = Color(0xFF1C1010),
            surfaceColor = Color(0xFF331B1B),
            mascotEmoji = "🏎️",
            tagline = "Speed down the turbo race track!",
            particleType = ParticleType.SPEED_LINES
        ),
        KidTheme(
            id = "sports",
            nameRes = com.example.R.string.theme_sports,
            emoji = "⚽",
            primaryColor = Color(0xFF3B82F6),
            secondaryColor = Color(0xFF1D4ED8),
            accentColor = Color(0xFF10B981),
            backgroundColor = Color(0xFF0B1B2B),
            surfaceColor = Color(0xFF142E47),
            mascotEmoji = "⚽",
            tagline = "Score a winning goal with your team!",
            particleType = ParticleType.SOCCER_BALLS
        ),
        KidTheme(
            id = "fantasy",
            nameRes = com.example.R.string.theme_fantasy,
            emoji = "🧙",
            primaryColor = Color(0xFFA855F7),
            secondaryColor = Color(0xFF7E22CE),
            accentColor = Color(0xFF38BDF8),
            backgroundColor = Color(0xFF1E0F2E),
            surfaceColor = Color(0xFF311849),
            mascotEmoji = "🦄",
            tagline = "Cast wonderous spells in the enchanted kingdom!",
            particleType = ParticleType.MAGIC_SPARKLES
        ),
        KidTheme(
            id = "ocean",
            nameRes = com.example.R.string.theme_ocean,
            emoji = "🌊",
            primaryColor = Color(0xFF06B6D4),
            secondaryColor = Color(0xFF0891B2),
            accentColor = Color(0xFF34D399),
            backgroundColor = Color(0xFF081C24),
            surfaceColor = Color(0xFF0F313E),
            mascotEmoji = "🐬",
            tagline = "Dive deep into calm underwater wonders!",
            particleType = ParticleType.BUBBLES
        ),
        KidTheme(
            id = "robots",
            nameRes = com.example.R.string.theme_robots,
            emoji = "🤖",
            primaryColor = Color(0xFF64748B),
            secondaryColor = Color(0xFF334155),
            accentColor = Color(0xFF06B6D4),
            backgroundColor = Color(0xFF0F172A),
            surfaceColor = Color(0xFF1E293B),
            mascotEmoji = "🤖",
            tagline = "Build and program high-tech robot friends!",
            particleType = ParticleType.ROBOT_GEARS
        ),
        KidTheme(
            id = "colorful",
            nameRes = com.example.R.string.theme_colorful,
            emoji = "🌈",
            primaryColor = Color(0xFFEC4899),
            secondaryColor = Color(0xFF8B5CF6),
            accentColor = Color(0xFFFBBF24),
            backgroundColor = Color(0xFF240E20),
            surfaceColor = Color(0xFF3D1636),
            mascotEmoji = "🎨",
            tagline = "A world of bright colors and creativity!",
            particleType = ParticleType.RAINBOW_CIRCLES
        ),
        KidTheme(
            id = "adventure",
            nameRes = com.example.R.string.theme_adventure,
            emoji = "🌳",
            primaryColor = Color(0xFF84CC16),
            secondaryColor = Color(0xFF4D7C0F),
            accentColor = Color(0xFFFB923C),
            backgroundColor = Color(0xFF141F0A),
            surfaceColor = Color(0xFF223512),
            mascotEmoji = "🦁",
            tagline = "Trek across the lush safari wildlands!",
            particleType = ParticleType.JUNGLE_LEAVES
        )
    )

    fun getThemeById(id: String): KidTheme {
        return themes.find { it.id == id } ?: themes[0]
    }
}

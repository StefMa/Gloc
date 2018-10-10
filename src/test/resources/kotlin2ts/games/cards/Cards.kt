package kotlin2ts.games.cards

import java.time.LocalDateTime

enum class Rarity(val abbreviation: String) {
    Normal("N"),
    Rare("R"),
    SuperRare("SR"),
}

data class Card(
        val ref: String,
        val rarity: Rarity,
        val name: String,
        val description: String,
        val command: String?,
        val playCard: (() -> Unit)?
) {
    val generatedTitleLine = "*$name* [$rarity]"
}

data class Inventory(
        val cards: List<Card> = listOf()
)

data class Player(
        val name: String,
        val inventory: Inventory = Inventory(),
        val achievementsProgress: List<AchievementCompletionState> = listOf(),
        val notices: List<Notice> = listOf()
)

data class Notice(
        val dateTime: LocalDateTime,
        val text: String
)

data class Achievement(
        val ref: String,
        val title: String,
        val description: String,
        val measuredProperty: (player: Player) -> Int,
        val neededValue: Int
)

data class AchievementCompletionState(
        val achievementRef: String,
        val reachedValue: Int
)
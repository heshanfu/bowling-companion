package ca.josephroque.bowlingcompanion.statistics.impl

import android.os.Parcel
import android.os.Parcelable
import ca.josephroque.bowlingcompanion.R
import ca.josephroque.bowlingcompanion.common.interfaces.parcelableCreator
import ca.josephroque.bowlingcompanion.games.Frame
import ca.josephroque.bowlingcompanion.games.Game
import ca.josephroque.bowlingcompanion.statistics.AverageStatistic
import ca.josephroque.bowlingcompanion.statistics.StatisticsCategory
import ca.josephroque.bowlingcompanion.statistics.provider.StatisticsUnit

/**
 * Copyright (C) 2018 Joseph Roque
 *
 * Average score in 15th game of series.
 */
class Game15AverageStatistic(override var total: Int, override var divisor: Int) : AverageStatistic {

    // MARK: Modifiers

    /** @Override */
    override fun modify(game: Game) {
        if (game.ordinal == 15) {
            divisor++
            total += game.score
        }
    }

    override val titleId = Id
    override val id = Id.toLong()
    override val category = StatisticsCategory.Average

    // MARK: Parcelable

    companion object {
        /** Creator, required by [Parcelable]. */
        @Suppress("unused")
        @JvmField val CREATOR = parcelableCreator(::Game15AverageStatistic)

        /** Unique ID for the statistic. */
        const val Id = R.string.statistic_average_15
    }

    /**
     * Construct this statistic from a [Parcel].
     */
    constructor(p: Parcel): this(total = p.readInt(), divisor = p.readInt())

    // MARK: Overrides

    /** @Override */
    override fun isModifiedBy(frame: Frame) = false

    /** @Override */
    override fun isModifiedBy(game: Game) = true

    /** @Override */
    override fun isModifiedBy(unit: StatisticsUnit) = false
}

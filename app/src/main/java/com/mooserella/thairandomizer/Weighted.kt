package com.mooserella.thairandomizer

import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random

/** Random picks where dimmed letters are less likely than normal ones. */
object Weighted {
    /**
     * How often a dimmed letter comes up compared to a normal one, for a slider value 0..100.
     * 0 gives 1 to 20, 50 gives 1 to 5 and 100 gives 1 to 2, in between on a logarithmic scale.
     */
    fun dimWeight(level: Int): Double {
        val s = level.coerceIn(0, 100) / 100.0
        val lnRatio = if (s <= 0.5) {
            ln(20.0) + (ln(5.0) - ln(20.0)) * (s / 0.5)
        } else {
            ln(5.0) + (ln(2.0) - ln(5.0)) * ((s - 0.5) / 0.5)
        }
        return 1.0 / exp(lnRatio)
    }

    /** Takes exactly one random number from [rnd], so seeded plans stay stable. */
    fun <T> pick(items: List<T>, isDim: (T) -> Boolean, level: Int, rnd: Random): T {
        val w = dimWeight(level)
        val weights = items.map { if (isDim(it)) w else 1.0 }
        var r = rnd.nextDouble() * weights.sum()
        for (i in items.indices) {
            r -= weights[i]
            if (r < 0) return items[i]
        }
        return items.last()
    }
}

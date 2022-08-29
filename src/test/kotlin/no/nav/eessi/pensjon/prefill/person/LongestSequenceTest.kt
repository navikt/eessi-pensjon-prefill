package no.nav.eessi.pensjon.prefill.person

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


data class Solution(val I: Int = 0, val N: Int = 0, val C: Char? = null)
operator fun Solution.compareTo(other: Solution) = this.N.compareTo(other.N)
operator fun Solution.plus(int: Int) = this.copy(N = N + int)

data class Accumulator(val candidate: Solution = Solution(), val best: Solution = Solution())

fun longestSequence(charSequence: CharSequence) =
    charSequence.foldIndexed(Accumulator(Solution(), Solution())) { index, (candidate, best), char ->
        when (candidate.C) {
            null -> Accumulator(
                candidate = Solution(index, 1, char),
                best = Solution(index, 1, char))
            char -> Accumulator(
                candidate = candidate + 1,
                best = if (candidate + 1 > best) candidate + 1 else best
            )
            else -> Accumulator(
                candidate = Solution(index, 1, char),
                best = if (candidate > best) candidate else best
            )
        }
    }.best

class LongestSequenceTest {
    @Test
    fun test() {
        assertEquals(Solution(10, 5, 'c'), longestSequence("ddaaaacccjcccccjjj"))
        assertEquals(Solution(2, 3, 'a'), longestSequence("ddaaacccaaa"))
        assertEquals(Solution(14, 6, 'a'), longestSequence("kjsfjsfajdsfjsaaaaaasssddfddddbbbdddaaa"))
        assertEquals(Solution(2, 3, 'b'), longestSequence("aabbb"))
        assertEquals(Solution(0, 0, null), longestSequence(""))
    }
}



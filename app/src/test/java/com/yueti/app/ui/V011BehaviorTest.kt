package com.yueti.app.ui

import com.yueti.app.data.VocabularyGroup
import com.yueti.app.music.MusicTrack
import com.yueti.app.music.MusicPlaybackPhase
import com.yueti.app.music.musicPlaybackPhase
import com.yueti.app.music.normalizedPlaybackProgress
import com.yueti.app.music.musicChromeTransform
import com.yueti.app.music.musicFanPose
import com.yueti.app.music.musicFanWindow
import com.yueti.app.music.musicArcWindow
import com.yueti.app.music.resolveMusicRevealTarget
import com.yueti.app.music.resolveMusicRevealRelease
import com.yueti.app.music.musicRevealProgressForDrag
import com.yueti.app.music.sanitizeCookie
import com.yueti.app.music.normalizeNeteaseMediaUrl
import com.yueti.app.music.normalizeNeteasePlaybackItems
import com.yueti.app.music.isMusicFallbackDnsHost
import com.yueti.app.music.nextResolvedQueueIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class V011BehaviorTest {
    @Test
    fun switchingVocabularyGroupAlwaysStartsANewDeckAtTheFirstWord() {
        val previous = VocabularyDeckCursor(
            revision = 7L,
            group = VocabularyGroup.All,
            currentIndex = 49,
        )

        val next = previous.switchTo(VocabularyGroup.Starred)

        assertEquals(8L, next.revision)
        assertEquals(VocabularyGroup.Starred, next.group)
        assertEquals(0, next.currentIndex)
    }

    @Test
    fun bingPickerBuildsStrictSearchAndRejectsInsecureImages() {
        val url = buildBingImageSearchUrl("apple", "苹果；苹果树")
        val uri = URI(url)
        assertEquals("https", uri.scheme)
        assertEquals("www.bing.com", uri.host)
        assertTrue(uri.rawQuery.contains("safeSearch=Strict"))
        assertTrue(uri.rawQuery.contains("apple"))

        assertTrue(validateBingImageUrl("https://tse1.mm.bing.net/th/id/OIP.example").isSuccess)
        assertFalse(validateBingImageUrl("http://example.com/apple.jpg").isSuccess)
        assertFalse(validateBingImageUrl("javascript:alert(1)").isSuccess)
    }

    @Test
    fun musicArcWindowContainsAtMostTwoTracksOnEitherSide() {
        val queue = (0 until 8).map { index ->
            MusicTrack(id = index.toString(), title = "Song $index", artist = "Artist")
        }

        val window = musicArcWindow(queue, currentIndex = 4)

        assertEquals(listOf(-2, -1, 0, 1, 2), window.map { it.relativeIndex })
        assertEquals(listOf("2", "3", "4", "5", "6"), window.map { it.track.id })
        assertEquals(listOf(-2, -1, 0), musicArcWindow(queue, 7).map { it.relativeIndex })
    }

    @Test
    fun neteaseCookiePersistenceKeepsOnlyTheAuthenticationAllowlist() {
        val sanitized = sanitizeCookie("MUSIC_U=token; __csrf=csrf; tracking=reject; os=pc; unsafe=1")

        assertTrue(sanitized.contains("MUSIC_U=token"))
        assertTrue(sanitized.contains("__csrf=csrf"))
        assertTrue(sanitized.contains("os=pc"))
        assertFalse(sanitized.contains("tracking"))
        assertFalse(sanitized.contains("unsafe"))
    }

    @Test
    fun musicNavigationMorphsContinuouslyInsteadOfAppearingAtRelease() {
        val collapsed = musicChromeTransform(0f)
        val halfway = musicChromeTransform(.5f)
        val expanded = musicChromeTransform(1f)

        assertEquals(64f, collapsed.heightDp, .01f)
        assertEquals(64f, halfway.heightDp, .01f)
        assertEquals(64f, expanded.heightDp, .01f)
        assertEquals(20f, expanded.cornerDp, .01f)
        assertTrue(halfway.playerReveal > 0f)
        assertTrue(halfway.playerReveal < 1f)
        assertTrue(halfway.iconTranslationYDp in 0f..18f)
        assertTrue(expanded.iconAlpha < .01f)
    }

    @Test
    fun musicNavigationSettlesByDistanceOrFlingDirection() {
        assertEquals(1f, resolveMusicRevealTarget(.46f, 0f), .01f)
        assertEquals(0f, resolveMusicRevealTarget(.30f, 0f), .01f)
        assertEquals(1f, resolveMusicRevealTarget(.12f, 1200f), .01f)
        assertEquals(0f, resolveMusicRevealTarget(.88f, -1200f), .01f)
    }

    @Test
    fun shortBottomEdgeDragCanStillRevealTheWholePlayer() {
        assertEquals(1f, musicRevealProgressForDrag(0f, 48f), .01f)
        assertEquals(0f, musicRevealProgressForDrag(1f, -48f), .01f)
    }

    @Test
    fun navigationEdgesHaveBoundedRubberBandOverdrag() {
        val beyondExpanded = musicRevealProgressForDrag(1f, 24f)
        val beyondCollapsed = musicRevealProgressForDrag(0f, -24f)

        assertTrue(beyondExpanded > 1f)
        assertTrue(beyondExpanded < 1.18f)
        assertTrue(beyondCollapsed < 0f)
        assertTrue(beyondCollapsed > -.18f)
    }

    @Test
    fun dockFanSharesAPivotAndFansOutWithDepth() {
        val collapsed = (-2..2).map { musicFanPose(it, 0f) }
        assertTrue(collapsed.all { it.xDp == 0f && it.yDp == 0f })

        val expanded = (-2..2).map { musicFanPose(it, 1f) }
        assertEquals(1f, expanded.single { it.relativeIndex == 0 }.scale, .01f)
        assertTrue(expanded.all { it.yDp <= 0f })
        assertTrue(expanded.minOf { it.yDp } >= -300f)
        assertTrue(expanded.filterNot { it.relativeIndex == 0 }.all { it.rotationDegrees < 0f })
        assertTrue(expanded.all { it.xDp == 0f })
        assertTrue(expanded.first().scale < expanded[2].scale)
        val orderedY = expanded.map { it.yDp }.sorted()
        assertTrue(orderedY.zipWithNext().all { (first, second) -> second - first >= 60f })
    }

    @Test
    fun dockFanUsesContiguousVisualSlotsAtQueueEdges() {
        val firstVisible = musicFanPose(relativeIndex = -2, expansion = 1f, visualSlot = 1)
        val secondVisible = musicFanPose(relativeIndex = -1, expansion = 1f, visualSlot = 2)

        assertEquals(64f, firstVisible.yDp - secondVisible.yDp, .01f)
        assertEquals(0f, firstVisible.xDp, .01f)
        assertEquals(0f, secondVisible.xDp, .01f)
    }

    @Test
    fun dockFanVirtualizesFourRealTracksWithoutEdgeHoles() {
        val queue = (0 until 50).map { index ->
            MusicTrack(id = index.toString(), title = "Song $index", artist = "Artist")
        }

        val middle = musicFanWindow(queue, playingIndex = 25, previewIndex = 25f)
        assertEquals(4, middle.size)
        assertEquals(listOf(24, 26, 23, 27), middle.map { it.queueIndex })
        assertEquals(listOf(1, 2, 3, 4), middle.map { it.visualSlot })

        val edge = musicFanWindow(queue, playingIndex = 0, previewIndex = 0f)
        assertEquals(listOf(1, 2, 3, 4), edge.map { it.queueIndex })
        assertEquals(listOf(1, 2, 3, 4), edge.map { it.visualSlot })

        assertTrue(musicFanWindow(emptyList(), -1, 0f).isEmpty())
        assertTrue(musicFanWindow(queue.take(1), 0, 0f).isEmpty())
        assertEquals(listOf(0), musicFanWindow(queue.take(2), 1, 1f).map { it.queueIndex })
        assertEquals(listOf(48, 47, 46, 45), musicFanWindow(queue, 49, 49f).map { it.queueIndex })
    }

    @Test
    fun miniPlayerProgressClampsInvalidPositionsAndDurations() {
        assertEquals(.25f, normalizedPlaybackProgress(15_000L, 60_000L), .001f)
        assertEquals(1f, normalizedPlaybackProgress(90_000L, 60_000L), .001f)
        assertEquals(0f, normalizedPlaybackProgress(-2L, 60_000L), .001f)
        assertEquals(0f, normalizedPlaybackProgress(1L, 0L), .001f)
    }

    @Test
    fun playbackPresentationNeverShowsPauseBeforeThePlayerActuallyPlays() {
        assertEquals(
            MusicPlaybackPhase.Buffering,
            musicPlaybackPhase(playbackState = 2, isPlaying = false, playWhenReady = true, hasError = false),
        )
        assertEquals(
            MusicPlaybackPhase.Playing,
            musicPlaybackPhase(playbackState = 3, isPlaying = true, playWhenReady = true, hasError = false),
        )
        assertEquals(
            MusicPlaybackPhase.ReadyPaused,
            musicPlaybackPhase(playbackState = 3, isPlaying = false, playWhenReady = false, hasError = false),
        )
        assertEquals(
            MusicPlaybackPhase.Error,
            musicPlaybackPhase(playbackState = 1, isPlaying = false, playWhenReady = false, hasError = true),
        )
    }

    @Test
    fun neteaseCdnHttpUrlsAreUpgradedInsteadOfDiscarded() {
        assertEquals(
            "https://m8.music.126.net/song.aac",
            normalizeNeteaseMediaUrl("http://m8.music.126.net/song.aac"),
        )
        assertEquals(
            "https://p1.music.126.net/cover.jpg",
            normalizeNeteaseMediaUrl("//p1.music.126.net/cover.jpg"),
        )
        val parsed = normalizeNeteasePlaybackItems(
            listOf("123" to "http://m8.music.126.net/song.aac"),
        )
        assertEquals("https://m8.music.126.net/song.aac", parsed["123"])
        assertEquals(null, normalizeNeteaseMediaUrl("https://example.com/not-netease.mp3"))
    }

    @Test
    fun releaseVelocityIsForwardedIntoTheRevealSpring() {
        val release = resolveMusicRevealRelease(.18f, 1200f)

        assertEquals(1f, release.target, .01f)
        assertTrue(release.initialVelocity > 0f)
        assertTrue(resolveMusicRevealRelease(.82f, -1200f).initialVelocity < 0f)
    }

    @Test
    fun fallbackDnsIsRestrictedToNeteaseApiAndMediaHosts() {
        assertTrue(isMusicFallbackDnsHost("music.163.com"))
        assertTrue(isMusicFallbackDnsHost("interface3.music.163.com"))
        assertTrue(isMusicFallbackDnsHost("p1.music.126.net"))
        assertTrue(isMusicFallbackDnsHost("m7.music.127.net"))
        assertFalse(isMusicFallbackDnsHost("example.com"))
        assertFalse(isMusicFallbackDnsHost("music.163.com.example.com"))
        assertFalse(isMusicFallbackDnsHost("www.bing.com"))
    }

    @Test
    fun unavailableSelectionAdvancesToTheNextResolvedTrackWithoutWrapping() {
        val queue = listOf("a", "b", "c", "d")
        val resolved = setOf("a", "d")

        assertEquals(3, nextResolvedQueueIndex(queue, resolved, requestedIndex = 1))
        assertEquals(3, nextResolvedQueueIndex(queue, resolved, requestedIndex = 3))
        assertEquals(-1, nextResolvedQueueIndex(queue, setOf("a"), requestedIndex = 1))
    }
}

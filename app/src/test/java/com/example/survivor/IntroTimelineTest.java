package com.example.survivor;

import org.junit.Test;
import static org.junit.Assert.*;

public class IntroTimelineTest {
    @Test public void recordingKeepsPictureAndTextHiddenForFifteenSeconds() {
        assertEquals(0, IntroTimeline.characters(14999));
        assertEquals(0f, IntroTimeline.pictureAlpha(14999), 0f);
        assertFalse(IntroTimeline.ready(14999));
    }
    @Test public void typingOccupiesSecondHalfAndContinueWaitsForThirtySeconds() {
        assertEquals(0, IntroTimeline.characters(15000));
        assertEquals(IntroTimeline.TEXT.length()/2, IntroTimeline.characters(22500));
        assertTrue(IntroTimeline.pictureAlpha(15900) > 0f);
        assertTrue(IntroTimeline.pictureAlpha(15900) < 1f);
        assertEquals(1f, IntroTimeline.pictureAlpha(18000), 0f);
        assertFalse(IntroTimeline.ready(29999));
        assertTrue(IntroTimeline.ready(30000));
        assertEquals(IntroTimeline.TEXT.length(), IntroTimeline.characters(30000));
        assertEquals(IntroTimeline.TEXT.length(), IntroTimeline.characters(31000));
    }
}

package com.rocket.radar;

import org.junit.Test;

import static org.junit.Assert.*;

import com.rocket.radar.eventmanagement.CreateEventModel;
import com.rocket.radar.eventmanagement.Section;

/**
 * Test the eventmanagement package.
 */
public class EventManagementTests {
    // WARN: This will need to be updated when validation is implemented.
    // probably need test for validating each section.
//    @Test
//    public void section_navigation() {
//        CreateEventModel model = new CreateEventModel();
//        // Check that the first section is the one that we want.
//        assertEquals(Section.firstSection, model.getSection());
//
//        // Make sure we are following the order defined in the section
//        // enum.
//        for (var section : Section.values()) {
//            assertEquals(section, model.getSection());
//            model.nextSection();
//        }
//
//        assertEquals(Section.lastSection, model.getSection());
//
//        // Try saturating the button
//        model.nextSection();
//        assertEquals(Section.lastSection, model.getSection());
//
//        // Ok now we go in reverse.
//    }
}
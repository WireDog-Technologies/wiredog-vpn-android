package com.wiredog.vpn.ui.screens.servers

/**
 * Hardcoded SVG positions for server cities on the US map.
 * SVG viewBox: 0 0 2000 1200
 */
object ServerMapPosition {

    data class Position(val x: Float, val y: Float)

    // SVG viewBox dimensions
    const val SVG_WIDTH = 2000f
    const val SVG_HEIGHT = 1200f

    // Hardcoded city positions on the SVG map
    private val positions = mapOf(
        "Atlanta" to Position(1505f, 775f),
        "Boston" to Position(1870f, 333f),
        "Chantilly" to Position(1700f, 525f),
        "Charlotte" to Position(1630f, 700f),
        "Chicago" to Position(1355f, 440f),
        "Columbus" to Position(1515f, 500f),
        "Dallas" to Position(1050f, 850f),
        "Denver" to Position(775f, 535f),
        "Honolulu" to Position(670f, 1058f),
        "Las Vegas" to Position(440f, 635f),
        "Los Angeles" to Position(315f, 720f),
        "Miami" to Position(1707f, 1095f),
        "Nashville" to Position(1390f, 690f),
        "Newark" to Position(1776f, 420f),
        "New York" to Position(1800f, 415f),
        "Philadelphia" to Position(1776f, 450f),
        "Phoenix" to Position(525f, 755f),
        "Portland" to Position(300f, 185f),
        "Richmond" to Position(1715f, 580f),
        "Salt Lake City" to Position(575f, 455f),
        "San Francisco" to Position(210f, 510f),
        "San Jose" to Position(210f, 510f),
        "Silicon Valley" to Position(210f, 510f),
        "Seattle" to Position(330f, 96f),
    )

    /**
     * Look up the hardcoded SVG position for a server city.
     * Returns null if the city has no mapped position.
     */
    fun getPosition(city: String): Position? {
        return positions[city]
    }
}

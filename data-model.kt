@Entity
data class Point(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    var planEast: Double = 0.0,      // metres after Bowditch + transformation
    var planNorth: Double = 0.0,
    var fixedLat: Double? = null,    // WGS84 from RTK
    var fixedLon: Double? = null,
    var isFixed: Boolean = false
)

@Entity
data class Connection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromPointId: Long,
    val toPointId: Long,
    val bearingDMS: String,          // "123.4555"
    val distanceM: Double,
    val isSkewBearing: Boolean = false   // Sabah-specific
)

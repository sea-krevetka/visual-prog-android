package entity

interface Movable {
    fun move(dt: Double)
    fun stop() {
        println("Объект остановился")
    }
}

interface Positionable {
    val x: Double
    val y: Double
    fun getPosition(): String {
        return "(${String.format("%.2f", x)}, ${String.format("%.2f", y)})"
    }
}

interface PersonalInfo {
    val fullName: String
    val age: Int
    
    fun getPersonalInfo(): String {
        return "$fullName, возраст: $age"
    }
    
}

interface Drivable : Movable {
    val licenseCategory: String
    val carSpeed: Double
    val direction: Double
    

    fun getLicenseInfo(): String {
        return "Категория прав: $licenseCategory"
    }

    fun canDrive(category: String): Boolean {
        return licenseCategory == category
    }
}
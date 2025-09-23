package entity

import manager.PositionManager
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random
import java.lang.reflect.Field

class Driver(
    fullName: String,
    age: Int,
    speed: Double = 5.0,
    private val licenseCategory: String = "B",
    private val carSpeed: Double = 10.0
) : Human(fullName, age, speed) {
    
    private var direction: Double = 0.0
    
    init {
        direction = Random.nextDouble(0.0, 2 * PI)
    }
    
    override fun move(dt: Double) {
        val originalX = getX()
        val originalY = getY()

        PositionManager.releasePosition(originalX, originalY)
        
        var newX = getX()
        var newY = getY()
        var newDirection = direction
        var attempts = 0
        val maxAttempts = 10
        
        while (attempts < maxAttempts) {
            if (Random.nextDouble() < 0.2) {
                val turn = if (Random.nextBoolean()) PI / 2 else -PI / 2
                newDirection = (direction + turn) % (2 * PI)
            }
            
            newX = getX() + carSpeed * dt * cos(newDirection)
            newY = getY() + carSpeed * dt * sin(newDirection)
            
            if (!PositionManager.isPositionOccupied(newX, newY)) {
                break
            }
            attempts++
            
            newDirection = (newDirection + PI / 2) % (2 * PI)
        }
        
        if (attempts >= maxAttempts) {
            newX = getX()
            newY = getY()
            newDirection = direction
        }
        
        if (PositionManager.occupyPosition(newX, newY)) {
            val humanClass = this::class.java.superclass
            val xField = humanClass.getDeclaredField("x")
            val yField = humanClass.getDeclaredField("y")
            
            xField.isAccessible = true
            yField.isAccessible = true
            
            xField.setDouble(this, newX)
            yField.setDouble(this, newY)
            
            direction = newDirection
        } else {
            PositionManager.occupyPosition(originalX, originalY)
        }
    }
    
    fun getLicenseCategory() = licenseCategory
    fun getCarSpeed() = carSpeed
    fun getDirection() = direction
    
    override fun toString(): String {
        return "${getFullName()} (водитель, возраст: ${getAge()}, права: $licenseCategory, " +
               "скорость машины: %.2f) → позиция (%.2f, %.2f), направление: %.2f°".format(
                   carSpeed, getX(), getY(), Math.toDegrees(direction))
    }
}
package entity

import manager.PositionManager
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

open class Human(
    override val fullName: String,
    override val age: Int,
    private var speed: Double = 1.0
) : Movable, Positionable, PersonalInfo {
    
    private var x: Double = 0.0
    private var y: Double = 0.0

    override val x: Double
        get() = this.x
        
    override val y: Double
        get() = this.y

    override fun move(dt: Double) {
        val originalX = x
        val originalY = y
        
        PositionManager.releasePosition(originalX, originalY)
        
        var newX = x
        var newY = y
        var attempts = 0
        val maxAttempts = 10
        
        while (attempts < maxAttempts) {
            val angle = Random.nextDouble(0.0, 2 * PI)
            newX = x + speed * dt * cos(angle)
            newY = y + speed * dt * sin(angle)
            
            if (!PositionManager.isPositionOccupied(newX, newY)) {
                break
            }
            attempts++
        }
        
        if (attempts >= maxAttempts) {
            newX = x
            newY = y
        }
        
        if (PositionManager.occupyPosition(newX, newY)) {
            x = newX
            y = newY
        } else {
            PositionManager.occupyPosition(originalX, originalY)
        }
    }

    fun getSpeed() = speed
    fun setSpeed(newSpeed: Double) { speed = newSpeed }

    override fun toString(): String {
        return "$fullName (возраст: $age, скорость: %.2f) → позиция (%.2f, %.2f)".format(speed, x, y)
    }
}
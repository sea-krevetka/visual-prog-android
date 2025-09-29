package entity

import manager.PositionManager
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

open class Human(
    private var fullName: String,
    private var age: Int,
    private var speed: Double = 1.0
) {
    private var x: Double = 0.0
    private var y: Double = 0.0

    open fun move(dt: Double) {
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

    fun getFullName() = fullName
    fun setFullName(name: String) { fullName = name }

    fun getAge() = age
    fun setAge(newAge: Int) { age = newAge }

    fun getSpeed() = speed
    fun setSpeed(newSpeed: Double) { speed = newSpeed }

    fun getX() = x
    fun getY() = y

    override fun toString(): String {
        return "$fullName (возраст: $age, скорость: %.2f) → позиция (%.2f, %.2f)".format(speed, x, y)
    }
}
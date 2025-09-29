package manager

import model.Position
import java.util.concurrent.locks.ReentrantLock

object PositionManager {
    private val occupiedPositions = mutableSetOf<Position>()
    private val lock = ReentrantLock()

    fun isPositionOccupied(x: Double, y: Double): Boolean {
        lock.lock()
        try {
            val roundedX = "%.2f".format(x).toDouble()
            val roundedY = "%.2f".format(y).toDouble()
            return occupiedPositions.any { 
                "%.2f".format(it.x).toDouble() == roundedX && 
                "%.2f".format(it.y).toDouble() == roundedY 
            }
        } finally {
            lock.unlock()
        }
    }

    fun occupyPosition(x: Double, y: Double): Boolean {
        lock.lock()
        try {
            val roundedX = "%.2f".format(x).toDouble()
            val roundedY = "%.2f".format(y).toDouble()
            
            if (isPositionOccupied(roundedX, roundedY)) {
                return false
            }
            
            occupiedPositions.add(Position(roundedX, roundedY))
            return true
        } finally {
            lock.unlock()
        }
    }

    fun releasePosition(x: Double, y: Double) {
        lock.lock()
        try {
            val roundedX = "%.2f".format(x).toDouble()
            val roundedY = "%.2f".format(y).toDouble()
            occupiedPositions.removeIf { 
                "%.2f".format(it.x).toDouble() == roundedX && 
                "%.2f".format(it.y).toDouble() == roundedY 
            }
        } finally {
            lock.unlock()
        }
    }

    fun clearAllPositions() {
        lock.lock()
        try {
            occupiedPositions.clear()
        } finally {
            lock.unlock()
        }
    }
}
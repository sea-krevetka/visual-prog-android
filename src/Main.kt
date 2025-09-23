import entity.Human
import entity.Driver
import manager.PositionManager
import util.*
import kotlin.concurrent.thread

fun main() {
    val humans = arrayOf(
        Human("Нейв Арлекино Пьеровна", 33, 1.2),
        Human("Каэдэхара Кадзуха Артемович", 28, 1.4),
        Human("Рагнвиндр Дилюк Крепусович", 22, 1.7),
        Human("Тарталья Аякс Анатольев", 24, 2.0),
        Human("Флинс Кирилл Чудомирович", 20, 1.9),
        Human("Ритинмунд Альбедо Рейндоттир", 18, 1.5)
    )

    val drivers = arrayOf(
        Driver("Альберич Кейя Александрович", 23, 1.2, "B", 8.0),
        Driver("Ран Нагиса Годфазерович", 21, 3.0, "C", 12.0),
        Driver("Иль Креветка Шуриковна", 16, 1.6, "B", 6.0),
        Driver("Санни Кевиновна Каслана", 25, 4.2, "D", 15.0),
        Driver("Фрогуна Таеся Кваковна", 52, 5.0, "BE", 18.0)
    )

    val simulationTime = 10.0
    val timeStep = 0.5

    PositionManager.clearAllPositions()

    println("Начало симуляции движения ${humans.size + drivers.size} объектов")
    println("Пешеходов: ${humans.size}, Водителей: ${drivers.size}")
    println("Время симуляции: $simulationTime секунд")
    println("=" * 50)

    val humanThread = thread {
        var currentTime = 0.0
        while (currentTime <= simulationTime) {
            humans.forEach { human ->
                human.move(timeStep)
            }
            currentTime += timeStep
            Thread.sleep((timeStep * 1000).toLong())
        }
    }

    val driverThread = thread {
        var currentTime = 0.0
        while (currentTime <= simulationTime) {
            drivers.forEach { driver ->
                driver.move(timeStep)
            }
            currentTime += timeStep
            Thread.sleep((timeStep * 1000).toLong())
        }
    }

    var currentTime = 0.0
    while (currentTime <= simulationTime) {
        Thread.sleep(500)
        
        println("\nВремя: %.1f сек".format(currentTime))
        println("-" * 50)
        
        println("ПЕШЕХОДЫ:")
        humans.forEach { human ->
            println("  $human")
        }
        
        println("\nВОДИТЕЛИ:")
        drivers.forEach { driver ->
            println("  $driver")
        }
        
        currentTime += 0.5
    }

    humanThread.join()
    driverThread.join()

    println("\n" + "=" * 50)
    println("Симуляция завершена!")

    println("\nФинальные позиции:")
    println("ПЕШЕХОДЫ:")
    humans.forEach { human ->
        println("  ${human.getFullName()}: (%.2f, %.2f)".format(human.getX(), human.getY()))
    }
    
    println("\nВОДИТЕЛИ:")
    drivers.forEach { driver ->
        println("  ${driver.getFullName()}: (%.2f, %.2f), направление: %.2f°".format(
            driver.getX(), driver.getY(), Math.toDegrees(driver.getDirection())))
    }
}
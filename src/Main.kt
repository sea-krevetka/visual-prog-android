import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

class Human(
    private var fullName: String,
    private var age: Int,
    private var speed: Double = 1.0
) {
    private var x: Double = 0.0
    private var y: Double = 0.0

    fun move(dt: Double) {
        val angle = Random.nextDouble(0.0, 2 * PI)
        x += speed * dt * cos(angle)
        y += speed * dt * sin(angle)
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

fun main() {
    val humans = arrayOf(
        Human("Нейв Арлекино Пьеровна", 33, 1.2),
        Human("Каэдэхара Кадзуха Артемович", 28, 1.4),
        Human("Рагнвиндр Дилюк Крепусович", 22, 1.7),
        Human("Тарталья Аякс Анатольев", 24, 2.0),
        Human("Флинс Кирилл Чудомирович", 20, 1.9),
        Human("Ритинмунд Альбедо Рейндоттир", 18, 1.5),
        Human("Альберич Кейя Александрович", 23, 1.2),
        Human("Ран Нагиса Годфазерович", 21, 3.0),
        Human("Иль Креветка Шуриковна", 16, 1.6),
        Human("Санни Кевиновна Каслана", 25, 4.2),
        Human("Фрогуна Таеся Кваковна", 52, 5.0)
    )

    val simulationTime = 10.0
    val timeStep = 0.5
    var currentTime = 0.0

    println("Начало симуляции движения ${humans.size} человек")
    println("Время симуляции: $simulationTime секунд")
    println("=" * 50)

    while (currentTime <= simulationTime) {
        println("\nВремя: %.1f сек".format(currentTime))
        println("-" * 30)

        humans.forEach { human ->
            human.move(timeStep)
            println(human)
        }

        currentTime += timeStep
        Thread.sleep(10)
    }

    println("\n" + "=" * 50)
    println("Симуляция завершена!")

    println("\nФинальные позиции:")
    humans.forEach { human ->
        println("${human.getFullName()}: (%.2f, %.2f)".format(human.getX(), human.getY()))
    }
}

operator fun String.times(n: Int): String = repeat(n)

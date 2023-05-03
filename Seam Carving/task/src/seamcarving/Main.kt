package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.sqrt

fun main(args: Array<String>) {
    val inName = args[1]
    val outName = args[3]
    val width = args[5].toInt()
    val height = args[7].toInt()
    var image = ImageIO.read(File(inName))
    repeat((1..width).count()) {
        image = writeAndDeleteSeam(image)
    }
    repeat((1..height).count()) {
        image = horizontalSeam(image)
    }
    ImageIO.write(image, "png", File(outName))
}

fun horizontalSeam(image: BufferedImage): BufferedImage {
    var flippedImage = flipImage90Degrees(image)
    flippedImage = writeAndDeleteSeam(flippedImage)
    (1..3).forEach { _ -> flippedImage = flipImage90Degrees(flippedImage) }
    return flippedImage
}

fun writeAndDeleteSeam(image: BufferedImage): BufferedImage {
    var newImage = writeSeam(image)
    newImage = compressImage(newImage)
    return newImage
}

fun writeSeam(image: BufferedImage): BufferedImage {
    val lastX = image.width - 1
    val lastY = image.height - 1

    val minEnergySum: Array<Array<Double>> = Array(image.width) { Array(image.height) { 0.0 } }
    for (y in 0..lastY) // Row by row fill top down
        for (x in 0..lastX) {
            val xd = x.coerceIn(1 until lastX) // Shift by 1 near the borders
            val yd = y.coerceIn(1 until lastY)

            val colorX1 = Color(image.getRGB(xd - 1, y))
            val colorX2 = Color(image.getRGB(xd + 1, y))
            val colorY1 = Color(image.getRGB(x, yd - 1))
            val colorY2 = Color(image.getRGB(x, yd + 1))

            // Don't need to store energy due to one pass
            val energyXY = sqrt(deltaSquare(colorX1, colorX2) + deltaSquare(colorY1, colorY2))

            // *** Part Two ***
            minEnergySum[x][y] = energyXY +
                    if (y > 0) {
                        val indices = when (x) { // Use 3 pixels one line above
                            0 -> 0..1       // .. sometimes 2
                            lastX -> x - 1..x
                            else -> x - 1..x + 1
                        }
                        indices.minOf { minEnergySum[it][y - 1] }
                    } else 0.0 // For first line it's just energy
        }

    // Take min sum on the bottom line and reconstruct the shortest path line by line bottom up
    var x = minEnergySum.indices.minByOrNull { minEnergySum[it][lastY] }!!
    image.setRGB(x, lastY, Color.RED.rgb)
    for (y in lastY - 1 downTo 0) {
        val indices = when (x) {
            0 -> 0..1
            lastX -> x - 1..x
            else -> x - 1..x + 1
        }
        x = indices.minByOrNull { minEnergySum[it][y] }!! // X where min sum in 3 (or 2) pixels on the prev line
        image.setRGB(x, y, Color.RED.rgb)
    }
    return image
}

fun flipImage90Degrees(image: BufferedImage): BufferedImage {
    val width = image.height
    val height = image.width
    val newImage = BufferedImage(width, height, image.type)

    for (row in 0 until height) {
        for (col in 0 until width) {
            newImage.setRGB(col, height - row - 1, image.getRGB(row, col))
        }
    }

    return newImage
}

fun deltaSquare(a: Color, b: Color): Double {
    return (a.red - b.red).toDouble().pow(2.0) +
            (a.green - b.green).toDouble().pow(2.0) +
            (a.blue - b.blue).toDouble().pow(2.0)
}

fun compressImage(image: BufferedImage): BufferedImage {
    var newWidth = image.width - 1
    val newImage = BufferedImage(newWidth, image.height, image.type)
    for (j in 0 until image.height) {
        var currentI = 0
        for (i in 0 until image.width) {
            val currentPixel = image.raster.getPixel(i, j, intArrayOf(0, 0, 0))
            val currentPixelColor = Color(currentPixel[0], currentPixel[1], currentPixel[2])
            if (currentPixelColor != Color.red) {
                newImage.raster.setPixel(currentI, j, currentPixel)
                currentI++
            }
        }
    }
    return newImage
}

fun isColRed(image: BufferedImage, col: Int): Boolean {
    for (row in 0 until image.height) {
        if (image.getRGB(col, row) != Color.RED.rgb) return false
    }
    return true
}
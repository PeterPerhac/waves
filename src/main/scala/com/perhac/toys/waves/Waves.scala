package com.perhac.toys.waves

import java.awt.Color
import java.awt.event.ActionEvent
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import javax.swing.Timer
import scala.swing._
import scala.swing.event.MouseMoved

case class Pixel(x: Int, y: Int) {
  def withinBounds(maxX: Int, maxY: Int): Boolean = x >= 0 && x < maxX && y >= 0 && y < maxY
  def translate(dX: Int, dY: Int): Pixel = Pixel(this.x + dX, this.y + dY)
}

object Waves extends SimpleSwingApplication {

  val W: Int = 1024
  val H: Int = 768

  class DataPanel(frame: MainFrame) extends Panel {

    val canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB)
    val canvasRectangle = new Rectangle2D.Float(0f, 0f, W.toFloat, H.toFloat)

    var mousePosition: Point = new Point(W / 2, H / 2)
    var phase: Double = 0.0d
    var doClear: Boolean = true
    var spacing: Int = 30
    var magnitude: Int = 90
    var maxPropagation: Int = 1200

    var theMesh: Array[Pixel] = _

    def clearCanvas(): Unit = {
      val g = canvas.createGraphics()
      g.setBackground(Color.BLACK)
      g.setColor(Color.BLACK)
      g.fill(canvasRectangle)
      g.dispose()
    }

    def doRefresh(): Unit = {
      phase = phase + 0.02
      if (phase > 1.0) phase = phase - 1.0
      if (doClear) {
        clearCanvas()
      }
      plotPoints()
      this.repaint()
    }

    listenTo(mouse.moves)
    focusable = true
    requestFocusInWindow()

    reactions += {
      case MouseMoved(_, point, _) => mousePosition = point
    }

    val makeStar: Pixel => List[Pixel] = pixel =>
      List(
        pixel, //keep original centre-point
        pixel.translate(1, 1), // diagonal neighbour
        pixel.translate(1, -1), // another
        pixel.translate(-1, 1), // another
        pixel.translate(-1, -1) // another
    )

    def translatePixel(mx: Int, my: Int, a: Double, m: Int)(p: Pixel): Pixel = {
      val dx = Math.abs(p.x - mx)
      val dy = Math.abs(p.y - my)
      val dist = Math.min(Math.sqrt(dx * dx + dy * dy), maxPropagation)

      val strength: Double = (maxPropagation - dist) / maxPropagation.toDouble

      val r: Double = m * strength
      val adjustedPhase = 1.0 - (a + strength)
      val rad = adjustedPhase * 2 * Math.PI

      p.translate(dX = (r * Math.sin(rad)).toInt, dY = (r * Math.cos(rad)).toInt)
    }

    def plotPoints(): Unit =
      theMesh
        .map(translatePixel(mousePosition.x, mousePosition.y, phase, magnitude))
        .flatMap(makeStar)
        .foreach(pixel =>
          if (pixel.withinBounds(W, H)) {
            canvas.setRGB(pixel.x, pixel.y, 0xFFFF00)
        })

    override def paintComponent(g: Graphics2D): Unit =
      g.drawImage(canvas, null, null)

    new Timer(20, (_: ActionEvent) => doRefresh()).start()

  }

  private def createStarField(d: Int): Array[Array[Pixel]] = {
    val hCount = (W / d) + 1
    val vCount = (H / d) + 1
    Array.tabulate(vCount) { yIdx =>
      Array.tabulate(hCount)(xIdx => Pixel(xIdx * d, yIdx * d))
    }
  }

  override def top: MainFrame = new MainFrame {
    contents = new DataPanel(this) {
      preferredSize = new Dimension(W, H)
      theMesh = createStarField(spacing).flatten
      doRefresh()
    }
  }
}

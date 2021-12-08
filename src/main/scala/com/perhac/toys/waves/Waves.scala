package com.perhac.toys.waves

import java.awt.Color
import java.awt.Color.getHSBColor
import java.awt.event.ActionEvent
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import javax.swing.Timer
import scala.collection.{immutable, mutable}
import scala.swing._
import scala.swing.event.{Key, KeyPressed, KeyReleased, MouseMoved}

case class Pixel(x: Int, y: Int) {
  def withinBounds(maxX: Int, maxY: Int): Boolean = x >= 0 && x < maxX && y >= 0 && y < maxY
  def translate(dX: Int, dY: Int): Pixel = Pixel(this.x + dX, this.y + dY)
}

object Waves extends SimpleSwingApplication {

  val W: Int = 1024
  val H: Int = 768

  class DataPanel(frame: MainFrame) extends Panel {

    def updateTitle(): Unit =
      frame.title =
        s"spacing: $spacing, " +
          s"dot effect level: $dotDistance, " +
          s"magnitude: $magnitude, " +
          s"speed: $speed, " +
          s"max propagation: $maxPropagation"

    private def createStarField(): Array[Array[Pixel]] = {
      val hCount = (W / spacing) + 1
      val vCount = (H / spacing) + 1
      Array.tabulate(vCount) { yIdx =>
        Array.tabulate(hCount)(xIdx => Pixel(xIdx * spacing, yIdx * spacing))
      }
    }

    val canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB)
    val canvasRectangle = new Rectangle2D.Float(0f, 0f, W.toFloat, H.toFloat)
    val autoResettableKeys: collection.immutable.Set[Key.Value] =
      immutable.HashSet(Key.Space, Key.Q, Key.T, Key.D, Key.E)

    var mousePosition: Point = new Point(W / 2, H / 2)
    val pressedKeys: collection.mutable.Set[Key.Value] = mutable.HashSet()

    var hue: Float = 1.0f
    var phase: Double = 0.0d
    var doClear: Boolean = true
    var spacing: Int = 10
    var magnitude: Int = 40
    var maxPropagation: Int = 500
    var tetherHueToRefresh: Boolean = true
    var color: Int = 0xFF0000
    var dotDistance: Int = 0
    var speed: Double = 0.01

    var theMesh: Array[Pixel] = createStarField().flatten

    def newColor(): Unit = color = getHSBColor(hue, 1.0f, 1.0f).getRGB

    def clearCanvas(): Unit = {
      val g = canvas.createGraphics()
      g.setBackground(Color.BLACK)
      g.setColor(Color.BLACK)
      g.fill(canvasRectangle)
      g.dispose()
    }

    def doRefresh(): Unit = {
      if (doClear) {
        clearCanvas()
      }

      if (tetherHueToRefresh) {
        hue = hue + 0.0075f
      }

      phase = phase + speed
      if (phase > 1.0) phase = phase - 1.0

      newColor()
      plotPoints()
      updateTitle()
      this.repaint()
    }

    listenTo(keys, mouse.moves)
    focusable = true
    requestFocusInWindow()

    reactions += {
      case MouseMoved(_, point, _) => mousePosition = point
      case KeyPressed(_, k, _, _)  => pressedKeys.addOne(k)
      case KeyReleased(_, k, _, _) => pressedKeys.subtractOne(k)
    }

    val makeStar: Pixel => List[Pixel] = pixel =>
      List(
        pixel, //keep original centre-point
        pixel.translate(dotDistance, dotDistance), // neighbours
        pixel.translate(dotDistance, -dotDistance),
        pixel.translate(-dotDistance, dotDistance),
        pixel.translate(-dotDistance, -dotDistance),
        pixel.translate(dotDistance, 0),
        pixel.translate(-dotDistance, 0),
        pixel.translate(0, dotDistance),
        pixel.translate(0, -dotDistance)
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
            canvas.setRGB(pixel.x, pixel.y, color)
        })

    override def paintComponent(g: Graphics2D): Unit =
      g.drawImage(canvas, null, null)

    new Timer(
      20,
      (_: ActionEvent) => {
        pressedKeys.foreach {
          k =>
            k match {
              case Key.Space => doClear = !doClear
              case Key.T     => tetherHueToRefresh = !tetherHueToRefresh
              case Key.Q     => frame.closeOperation()
              case Key.D     => dotDistance = dotDistance + 1
              case Key.E     => dotDistance = dotDistance - 1
              case Key.A     => magnitude = magnitude - 1
              case Key.S     => magnitude = magnitude + 1
              case Key.Z     => maxPropagation = maxPropagation - 1
              case Key.X     => maxPropagation = maxPropagation + 1
              case Key.O =>
                if (spacing > 3) {
                  spacing = spacing - 1
                  theMesh = createStarField().flatten
                }
              case Key.I =>
                spacing = spacing + 1
                theMesh = createStarField().flatten
              case Key.Minus  => speed = speed - 0.001
              case Key.Equals => speed = speed + 0.001
              case _          => // do nothing
            }
            if (autoResettableKeys.contains(k)) pressedKeys.subtractOne(k)
        }
        doRefresh()
      }
    ).start()

  }

  override def top: MainFrame = new MainFrame {
    contents = new DataPanel(this) {
      preferredSize = new Dimension(W, H)
      doRefresh()
    }
  }
}

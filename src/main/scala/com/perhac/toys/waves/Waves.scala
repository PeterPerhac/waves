package com.perhac.toys.waves

import java.awt.Color
import java.awt.Color.getHSBColor
import java.awt.event.ActionEvent
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import javax.swing.Timer
import scala.collection.{immutable, mutable}
import scala.swing._
import scala.swing.event.{Key, KeyPressed, KeyReleased, MouseMoved, UIElementResized}

case class Pixel(x: Int, y: Int) {
  def withinBounds(maxX: Int, maxY: Int): Boolean = x >= 0 && x < maxX && y >= 0 && y < maxY
  def translate(dX: Int, dY: Int): Pixel = Pixel(this.x + dX, this.y + dY)
}

// @formatter:off
case class Configuration(hue: Float, phase: Double, doClear: Boolean, pause: Boolean, spacing: Int, magnitude: Int, maxPropagation: Int, tetherHueToRefresh: Boolean, color: Int, dotDistance: Int, speed: Double)
object DefaultConfiguration extends Configuration(hue = 1.0f, phase = 0.0d, doClear = true, pause = false, spacing = 10, magnitude = 50, maxPropagation = 300, tetherHueToRefresh = true, color = 0xFF0000, dotDistance = 0, speed = 0.01)
// @formatter:on

object Waves extends SimpleSwingApplication {

  var W: Int = 1024
  var H: Int = 768

  class WaveContainer(frame: MainFrame) extends Panel {

    def updateTitle(): Unit =
      frame.title =
        s"spacing: $spacing, " +
          s"dot effect level: $dotDistance, " +
          s"magnitude: $magnitude, " +
          f"speed: $speed%.3f, " +
          s"max propagation: $maxPropagation"

    private def createStarField(): Array[Pixel] = {
      val hCount = (W / spacing) + 1
      val vCount = (H / spacing) + 1
      Array
        .tabulate(vCount)(yIdx => Array.tabulate(hCount)(xIdx => Pixel(xIdx * spacing, yIdx * spacing)))
        .flatten
    }

    var canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB)
    var canvasRectangle = new Rectangle2D.Float(0f, 0f, W.toFloat, H.toFloat)

    def newCanvas(): Unit = {
      canvas.getGraphics.dispose()
      canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB)
      canvasRectangle = new Rectangle2D.Float(0f, 0f, W.toFloat, H.toFloat)
    }
    val autoResettableKeys: collection.immutable.Set[Key.Value] =
      immutable.HashSet(Key.Space, Key.Q, Key.R, Key.T, Key.D, Key.F, Key.P)

    var mousePosition: Point = new Point(W / 2, H / 2)
    val pressedKeys: collection.mutable.Set[Key.Value] = mutable.HashSet()

    var hue: Float = DefaultConfiguration.hue
    var phase: Double = DefaultConfiguration.phase
    var doClear: Boolean = DefaultConfiguration.doClear
    var pause: Boolean = DefaultConfiguration.pause
    var spacing: Int = DefaultConfiguration.spacing
    var magnitude: Int = DefaultConfiguration.magnitude
    var maxPropagation: Int = DefaultConfiguration.maxPropagation
    var tetherHueToRefresh: Boolean = DefaultConfiguration.tetherHueToRefresh
    var color: Int = DefaultConfiguration.color
    var dotDistance: Int = DefaultConfiguration.dotDistance
    var speed: Double = DefaultConfiguration.speed

    var theMesh: Array[Pixel] = createStarField()

    def reset(): Unit = {
      hue = DefaultConfiguration.hue
      phase = DefaultConfiguration.phase
      doClear = DefaultConfiguration.doClear
      pause = DefaultConfiguration.pause
      spacing = DefaultConfiguration.spacing
      magnitude = DefaultConfiguration.magnitude
      maxPropagation = DefaultConfiguration.maxPropagation
      tetherHueToRefresh = DefaultConfiguration.tetherHueToRefresh
      color = DefaultConfiguration.color
      dotDistance = DefaultConfiguration.dotDistance
      speed = DefaultConfiguration.speed
      theMesh = createStarField()
      updateTitle()
      doRefresh()
    }

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
      this.repaint()
    }

    listenTo(keys, mouse.moves, frame)
    focusable = true
    requestFocusInWindow()

    reactions += {
      case MouseMoved(_, point, _) => mousePosition = point
      case KeyPressed(_, k, _, _)  => pressedKeys.addOne(k)
      case KeyReleased(_, k, _, _) => pressedKeys.subtractOne(k)
      case UIElementResized(source) =>
        W = source.size.width
        H = source.size.height
        newCanvas()
        theMesh = createStarField()
    }

    val makeStar: Pixel => List[Pixel] = pixel => {
      if (dotDistance > 0)
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
      else List(pixel)
    }

    def translatePixel(mx: Int, my: Int, a: Double, m: Int)(p: Pixel): Pixel = {
      val dx = Math.abs(p.x - mx)
      val dy = Math.abs(p.y - my)
      val dist = Math.min(Math.sqrt(dx * dx + dy * dy), maxPropagation)

      val linearStrength: Double = (maxPropagation - dist) / maxPropagation.toDouble
      val strength: Double = Math.cos(0.5 * Math.PI * (1 - linearStrength))

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
              case Key.Q     => frame.closeOperation()
              case Key.R     => reset()
              case Key.T     => tetherHueToRefresh = !tetherHueToRefresh
              case Key.D     => if (dotDistance > 0) dotDistance = dotDistance - 1
              case Key.F     => dotDistance = dotDistance + 1
              case Key.A     => magnitude = magnitude - 1
              case Key.S     => magnitude = magnitude + 1
              case Key.Z     => maxPropagation = maxPropagation - 1
              case Key.X     => maxPropagation = maxPropagation + 1
              case Key.P     => pause = !pause
              case Key.O =>
                if (spacing > 2) {
                  spacing = spacing - 1
                  theMesh = createStarField()
                }
              case Key.I =>
                spacing = spacing + 1
                theMesh = createStarField()
              case Key.Minus  => speed = speed - 0.001
              case Key.Equals => speed = speed + 0.001
              case _          => // do nothing
            }
            if (autoResettableKeys.contains(k)) pressedKeys.subtractOne(k)
        }
        updateTitle()
        if (!pause) doRefresh()
      }
    ).start()

  }

  override def top: MainFrame = new MainFrame {
    contents = new WaveContainer(this) {
      preferredSize = new Dimension(W, H)
      doRefresh()
    }
  }
}

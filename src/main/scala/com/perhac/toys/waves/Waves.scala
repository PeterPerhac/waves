package com.perhac.toys.waves

import java.awt.Color
import java.awt.event.ActionEvent
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import javax.swing.Timer
import scala.swing._
import scala.swing.event.MouseMoved

case class Point(x: Int, y: Int) {
  def withinBounds(maxX: Int, maxY: Int): Boolean = x >= 0 && x < maxX && y >= 0 && y < maxY
}

object Waves extends SimpleSwingApplication {

  val A: Int = 1024

  class DataPanel(frame: MainFrame) extends Panel {

    val canvas = new BufferedImage(A, A, BufferedImage.TYPE_INT_RGB)
    val canvasRectangle = new Rectangle2D.Float(0f, 0f, A.toFloat, A.toFloat)

    var mousePosition: java.awt.Point = new java.awt.Point(A / 2, A / 2)
    var phase: Double = 0.0d
    var doClear: Boolean = true

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
      plotPoints()
      this.repaint()
    }

    listenTo(mouse.moves)
    focusable = true
    requestFocusInWindow()

    reactions += {
      case MouseMoved(_, point, _) => mousePosition = point
    }


    def plotPoints(): Unit =
      Seq.tabulate(100)(i => Point( // TODO somehow calculate the whole show
        x = mousePosition.x + i,
        y = mousePosition.y + i
      ))
        .foreach(pixel => if (pixel.withinBounds(A, A)) {
          canvas.setRGB(pixel.x, pixel.y, 0xFF0000)
        })

    override def paintComponent(g: Graphics2D): Unit =
      g.drawImage(canvas, null, null)

    new Timer(20, (_: ActionEvent) => doRefresh()).start()

  }

  override def top: MainFrame = new MainFrame {
    contents = new DataPanel(this) {
      preferredSize = new Dimension(A, A)
      doRefresh()
    }
  }
}

package client.draw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * 担当B（お絵描き）のキャンバスUI。
 *
 * ・マウスのドラッグを検知して線を描く（ローカル描画）
 * ・引いた線分(x1,y1,x2,y2,色)を DrawController.sendLine() でサーバーへ送る
 * ・サーバー経由で届いた他人の線を drawRemoteLine() で描く
 *
 * 描いた内容は BufferedImage に焼き付けて保持するので、
 * ウインドウの再描画やリサイズでも消えない。
 */
public class DrawPanel extends JPanel {

    private static final int CANVAS_W = 600;
    private static final int CANVAS_H = 400;
    private static final float STROKE = 2.0f;

    private BufferedImage canvas;   // 描画内容を保持する裏画像
    private Graphics2D g2;          // canvas への描画用
    private Point prev;             // 直前のマウス位置
    private String currentColor = "BLACK";

    public DrawPanel() {
        setPreferredSize(new Dimension(CANVAS_W, CANVAS_H));
        setBackground(Color.WHITE);
        initCanvas();

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                prev = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point cur = e.getPoint();
                if (prev != null) {
                    // 1) 自分の画面にすぐ描く
                    drawLine(prev.x, prev.y, cur.x, cur.y, currentColor);
                    // 2) サーバーへ送って他の人にも描いてもらう
                    DrawController.sendLine(prev.x, prev.y, cur.x, cur.y, currentColor);
                }
                prev = cur;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                prev = null;
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private void initCanvas() {
        canvas = new BufferedImage(CANVAS_W, CANVAS_H, BufferedImage.TYPE_INT_RGB);
        g2 = canvas.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        clearCanvas(); // 白で塗りつぶし
    }

    /** 1本の線分を裏画像に描いて再描画する（自分・他人共通の描画処理）。 */
    private void drawLine(int x1, int y1, int x2, int y2, String colorName) {
        g2.setColor(toColor(colorName));
        g2.drawLine(x1, y1, x2, y2);
        repaint();
    }

    /** サーバー経由で届いた他人の線を描く（DrawController からEDTで呼ばれる）。 */
    public void drawRemoteLine(int x1, int y1, int x2, int y2, String colorName) {
        drawLine(x1, y1, x2, y2, colorName);
    }

    /** ペンの色を切り替える。"WHITE" を選べば実質的に消しゴムになる。 */
    public void setColorName(String colorName) {
        this.currentColor = colorName;
    }

    /** キャンバスを白紙に戻す（このクライアントのみ）。 */
    public void clearCanvas() {
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, CANVAS_W, CANVAS_H);
        repaint();
        // TODO（チーム合意が必要）：全員の画面を同時に消すには
        //   Protocol に DRAW_CLEAR コマンドを追加し、ここから送信する。
    }

    /** 色名(文字列) -> Color。Protocolの色表現に合わせて増やせる。 */
    private Color toColor(String name) {
        String n = (name == null) ? "" : name.trim().toUpperCase();
        switch (n) {
            case "RED":    return Color.RED;
            case "BLUE":   return Color.BLUE;
            case "GREEN":  return new Color(0, 150, 0);
            case "YELLOW": return new Color(230, 200, 0);
            case "WHITE":  return Color.WHITE;   // 消しゴム
            case "BLACK":
            default:       return Color.BLACK;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(canvas, 0, 0, null);
    }
}

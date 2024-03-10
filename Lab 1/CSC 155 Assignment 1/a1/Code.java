package a1;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.lang.Math;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.util.*;

public class Code extends JFrame implements GLEventListener, KeyListener, MouseWheelListener, ActionListener
{	private GLCanvas myCanvas;
	private int renderingProgram;
	private int vao[] = new int[1];

	private float x = 0.0f;
	private float y = 0.0f;
	private float prevX = 0.0f;
	private float prevY = 0.0f;
	private float inc = 0.01f;

	// Variables for movement based on elapsed time.
	private double timeFactor;
	private double startTime;
	private double elapsedTime;
	private double prevCallTime = 0;
	
	// Counters for number of times color button or 1 key were pressed.
	private int key1PressCount = 0;
	private int colorButtonCount = 0;
	
	// Scale factor for scroll wheel.
	private float scaleFactor = 1.0f;
	
	// Boolean for triangle circle movement.
	private boolean toggleCircleClicked = false;

	public Code()
	{	setTitle("Assignment #1");
		setSize(800, 400);	// Default: 400, 200
		
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		myCanvas.addKeyListener(this);
		myCanvas.addMouseWheelListener(this);
		
		this.add(myCanvas);
		
		// Create and add JPanel.
		JPanel topPanel = new JPanel();
		this.add(topPanel, BorderLayout.NORTH);
		
		// Create, add, JButtons.  Add action listeners to JButtons.
		JButton circleMove = new JButton("Toggle Circle Move");
		JButton colorToggle = new JButton("Toggle Color");
		topPanel.add(circleMove);
		topPanel.add(colorToggle);
		circleMove.addActionListener(this);
		colorToggle.addActionListener(this);
				
		this.setVisible(true);
		
		Animator animator = new Animator(myCanvas);
		animator.start();
	} // end code

	public void display(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glUseProgram(renderingProgram);
		
		// Movement based on elapsed time.
		if(prevCallTime == 0)
			elapsedTime = System.currentTimeMillis() - startTime;
		else
			elapsedTime = System.currentTimeMillis() - prevCallTime;
		
		prevCallTime = System.currentTimeMillis();
		
		timeFactor = elapsedTime / 1000.0;

		// Move left to right when false.  Move in a circle when true.
		if(!toggleCircleClicked) {
			x += inc;
		
			if (x > 1.0f) inc = -(float)(timeFactor % 0.04f);
			if (x < -1.0f) inc = (float)(timeFactor % 0.04f);
		} // end if
		else {
			inc = (float)(timeFactor);
			
			prevX = x;
			prevY = y;
			x = prevX * (float)Math.cos(inc) - prevY * (float)Math.sin(inc);
			y = prevX * (float)Math.sin(inc) + prevY * (float)Math.cos(inc);
		} // end else
		
		int offsetLocX = gl.glGetUniformLocation(renderingProgram, "offsetX");
		int offsetLocY = gl.glGetUniformLocation(renderingProgram, "offsetY");
		int key1Count = gl.glGetUniformLocation(renderingProgram, "key1PressCount");
		int colorCount = gl.glGetUniformLocation(renderingProgram, "colorButtonCount");
		int sf = gl.glGetUniformLocation(renderingProgram, "scaleFactor");
		gl.glProgramUniform1f(renderingProgram, offsetLocX, x);
		gl.glProgramUniform1f(renderingProgram, offsetLocY, y);
		gl.glProgramUniform1i(renderingProgram, key1Count, key1PressCount);
		gl.glProgramUniform1i(renderingProgram, colorCount, colorButtonCount);
		gl.glProgramUniform1f(renderingProgram, sf, scaleFactor);

		gl.glDrawArrays(GL_TRIANGLES,0,3);
	}// end display
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// Detect if "Toggle Circle Move" JButton is clicked and flips the boolean toggleCircleClicked.
		if(e.getActionCommand().equals("Toggle Circle Move"))
			toggleCircleClicked = !toggleCircleClicked;
		
		// Detect if "Toggle Color" JButton is clicked and increment colorButtonCount counter.
		if(e.getActionCommand().equals("Toggle Color"))
			colorButtonCount++;
	} // end actionPerformed
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		// Increase/decrease scaleFactor based on mouse wheel movement.  Sets scaleFactor to 0.1f if
		// scaleFactor is ever less than 0.1f.
		scaleFactor += (float)(e.getWheelRotation() % 0.1f);
		
		if(scaleFactor < 0.1f)
			scaleFactor = 0.1f;
	} // end mouseWheelMoved
	
	@Override
	public void keyPressed(KeyEvent e) {
		switch(e.getKeyCode()) {
			// Increment key1PressCount counter when the "1" key is pressed.
			case KeyEvent.VK_1:
				key1PressCount++;
				break;
			// Exit the program when "Esc" is pressed.
			case KeyEvent.VK_ESCAPE:
				System.exit(0);
				break;
		} // end switch
	} // end keyPressed
	
	@Override
	public void keyTyped(KeyEvent e) {
		
	} // end keyTyped
	
	@Override
	public void keyReleased(KeyEvent e) {
	
	} // end keyReleased

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		startTime = System.currentTimeMillis();
		renderingProgram = Utils.createShaderProgram("a1/vertShader.glsl", "a1/fragShader.glsl");
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		
		// Print the current JOGL and OpenGL versions to the console at startup.
		System.out.println("\nJOGL Version: " + Package.getPackage("com.jogamp.opengl").getImplementationVersion());
		System.out.println("OpenGL Version: " + gl.glGetString(GL_VERSION) + "\n");
	} // end init

	public static void main(String[] args) { new Code(); } // end main
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {} // end reshape
	public void dispose(GLAutoDrawable drawable) {} // end dispose
} // end Code
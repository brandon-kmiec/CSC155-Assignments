package a2;

import java.nio.*;
import java.lang.Math;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.util.*;
import org.joml.*;

public class Code extends JFrame implements GLEventListener, KeyListener
{	private GLCanvas myCanvas;
	private int renderingProgram, axesProgram;
	private int vao[] = new int[1];
	private int vbo[] = new int[9];
	private float cameraX, cameraY, cameraZ;
	private float cubeLocX, cubeLocY, cubeLocZ;
	private float pyrLocX, pyrLocY, pyrLocZ;
	private float octLocX, octLocY, octLocZ;
	private float impLocX, impLocY, impLocZ;
	
	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4f mvMat = new Matrix4f(); // model-view matrix
	private int mvLoc, pLoc, mvLocAxes, pLocAxes;
	private float aspect;
	
	// Variables for movement based on elapsed time.
	private double timeFactor;
	private double startTime;
	private double elapsedTime;
	
	private int numObjVertices;
	private ImportedModel myModel;
	
	private int cubeTexture, pyramidTexture, octTexture, impTexture;
	
	private boolean axesVisible = true;
	
	private Camera camera;

	public Code()
	{	setTitle("Assignment #2");
		setSize(600, 600);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		this.add(myCanvas);
		this.setVisible(true);
		myCanvas.addKeyListener(this);
		
		Animator animator = new Animator(myCanvas);
		animator.start();
	}

	public void display(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		
		// Movement based on elapsed time.
		elapsedTime = System.currentTimeMillis() - startTime;
		timeFactor = elapsedTime / 1000.0;

		gl.glUseProgram(renderingProgram);
		mvLoc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgram, "p_matrix");		

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		vMat.translation(-cameraX,-cameraY,-cameraZ);
		//camera.setN(new Vector3f(0.0f, 0.0f, -2.0f));
		vMat = camera.getViewMatrix();
		//vMat.rotateXYZ(0.0f, (float)timeFactor, 0.0f);

		drawShapes(gl, timeFactor);
		
		gl.glUseProgram(axesProgram);
		mvLocAxes = gl.glGetUniformLocation(axesProgram, "mvMatrix");
		pLocAxes = gl.glGetUniformLocation(axesProgram, "pMatrix");
		mMat.translation(0.0f, 0.0f, 0.0f);
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		if(axesVisible)
			gl.glDrawArrays(GL_LINES, 0, 6);
	}
	
	@Override
	public void keyPressed(KeyEvent e)
	{	
		switch (e.getKeyCode())
		{	
			case KeyEvent.VK_W:
				camera.moveCamForward(1.0f);
				break;
			case KeyEvent.VK_S:
				camera.moveCamBackward(1.0f);
				break;
			case KeyEvent.VK_A:
				camera.moveCamLeft(1.0f);
				break;
			case KeyEvent.VK_D:
				camera.moveCamRight(1.0f);
				break;
			case KeyEvent.VK_Q:
				camera.moveCamUp(1.0f);
				break;
			case KeyEvent.VK_E:
				camera.moveCamDown(1.0f);
				break;
			case KeyEvent.VK_RIGHT:
				camera.yawCamRight((float)Math.toRadians(1.0));
				break;
			case KeyEvent.VK_LEFT:
				camera.yawCamLeft((float)Math.toRadians(1.0));
				break;
			case KeyEvent.VK_UP:
				camera.pitchCamUp((float)Math.toRadians(1.0));
				break;
			case KeyEvent.VK_DOWN:
				camera.pitchCamDown((float)Math.toRadians(1.0));
				break;
			case KeyEvent.VK_COMMA:
				camera.rollCamLeft((float)Math.toRadians(1.0));
				break;
			case KeyEvent.VK_PERIOD:
				camera.rollCamRight((float)Math.toRadians(1.0));
				break;
			case KeyEvent.VK_SPACE:
				axesVisible = !axesVisible;
				break;
			case KeyEvent.VK_ESCAPE:
				System.exit(0);
				break;
		}
		//super.keyPressed(e);
	} // end keyPressed
	
	@Override
	public void keyReleased(KeyEvent e)
	{
	} // end keyReleased
	
	@Override
	public void keyTyped(KeyEvent e)
	{
	} // end keyTyped
	
	private void drawShapes(GL4 gl, double timeFactor) {
		// draw the cube using buffer #0 and texture using buffer #1
		mMat.translation(cubeLocX, cubeLocY, cubeLocZ);
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
			// vertices for cube
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
			// texture for cube
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, cubeTexture);
			// draw cube
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
			// texture tiling
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_MIRRORED_REPEAT);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_MIRRORED_REPEAT);
		
		// draw the pyramid using buffer #2 and texture using buffer #3
		mMat.translation((float)Math.sin(timeFactor)*5.0f, (float)Math.cos(timeFactor)*(float)Math.sin(timeFactor)*5.0f + 2.0f, -3.0f);
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
			// vertices for pyramid
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
			// texture for pyramid
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, pyramidTexture);
			// draw pyramid
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 18);
		
		// draw the manual octagon object using buffer #4 and texture using buffer #5
		mMat.translation(octLocX, octLocY, octLocZ);
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
			// vertices for manual octagon object
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
			// texture for manual octagon object
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, octTexture);
			// draw manual octagon object
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 192);
			// texture tiling
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		
		// draw the imported object using buffer #6 and texture using buffer #7
		mMat.translation(impLocX, impLocY, impLocZ);
		mMat.rotateXYZ(0.75f*(float)timeFactor, 0.0f, 0.75f*(float)timeFactor);
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
			// vertices for imported object
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
			// texture for imported object
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, impTexture);
			// draw imported object
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, myModel.getNumVertices());
		
	} // end drawShapes

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		startTime = System.currentTimeMillis();
	
		renderingProgram = Utils.createShaderProgram("a2/vertShader.glsl", "a2/fragShader.glsl");
		axesProgram = Utils.createShaderProgram("a2/axesVertShader.glsl", "a2/axesFragShader.glsl");
		
		myModel = new ImportedModel("complexCube.obj");
		
		setupVertices();
		
		cameraX = 0.0f; cameraY = 0.0f; cameraZ = 8.0f;
		cubeLocX = -3.0f; cubeLocY = -3.0f; cubeLocZ = 0.0f;
		pyrLocX = 2.0f; pyrLocY = 2.0f; pyrLocZ = -3.0f;
		octLocX = 3.0f; octLocY = -3.0f; octLocZ = 0.0f;
		impLocX = 0.0f;	impLocY = 2.0f;	impLocZ = -10.0f;
				
		cubeTexture = Utils.loadTexture("customTexture.png");
		pyramidTexture = Utils.loadTexture("brick1.jpg");
		octTexture = Utils.loadTexture("wrinkled-page.jpg");
		impTexture = Utils.loadTexture("ice.jpg");
		
		camera = new Camera();
		camera.setCamLocation(new Vector3f(cameraX, cameraY, cameraZ));
	}

	private void setupVertices()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		
		//cube vertex coordinates and texture coordinates
		float[] cubePositions =
		{	
			-1.0f,  1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, 1.0f,  1.0f, -1.0f, -1.0f,  1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, 1.0f, -1.0f,  1.0f, 1.0f,  1.0f, -1.0f,
			1.0f, -1.0f,  1.0f, 1.0f,  1.0f,  1.0f, 1.0f,  1.0f, -1.0f,
			1.0f, -1.0f,  1.0f, -1.0f, -1.0f,  1.0f, 1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f, -1.0f,  1.0f,  1.0f, 1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f, -1.0f, -1.0f, -1.0f, -1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f, -1.0f, -1.0f,  1.0f, -1.0f, -1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f,  1.0f, -1.0f,  1.0f,  1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,  1.0f,
			-1.0f,  1.0f, -1.0f, 1.0f,  1.0f, -1.0f, 1.0f,  1.0f,  1.0f,
			1.0f,  1.0f,  1.0f, -1.0f,  1.0f,  1.0f, -1.0f,  1.0f, -1.0f
		};
		float[] cubeTexCoords = new float[]
		{ 
			1.0f, 1.0f,  1.0f, 0.0f,  0.0f, 0.0f, // back face
			0.0f, 0.0f,  0.0f, 1.0f,  1.0f, 1.0f,
			1.0f, 0.0f,  0.0f, 0.0f,  1.0f, 1.0f, // right face
			0.0f, 0.0f,  0.0f, 1.0f,  1.0f, 1.0f,
			1.0f, 0.0f,  0.0f, 0.0f,  1.0f, 1.0f, // front face
			0.0f, 0.0f,  0.0f, 1.0f,  1.0f, 1.0f,
			1.0f, 0.0f,  0.0f, 0.0f,  1.0f, 1.0f, // left face
			0.0f, 0.0f,  0.0f, 1.0f,  1.0f, 1.0f,
			0.0f, 1.0f,  1.0f, 1.0f,  1.0f, 0.0f, // bottom face
			1.0f, 0.0f,  0.0f, 0.0f,  0.0f, 1.0f,
			0.0f, 1.0f,  1.0f, 1.0f,  1.0f, 0.0f, // top face
			1.0f, 0.0f,  0.0f, 0.0f,  0.0f, 1.0f 
		};
		
		// pyramid vertex coordinates and texture coordinates
		float[] pyramidPositions =
		{	
			-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f,    //front
			1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 0.0f,    //right
			1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 0.0f,  //back
			-1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f,  //left
			-1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, //LF
			1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f  //RR
		};
		float[] pyrTextureCoordinates =
		{	
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
			0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f
		};
		
		// octagon manual object vertex coordinates and texture coordinates
		float[] octPos = new float[]
		{ 
			//bottom outside octagon
			-0.5f, -1.0f, 1.0f,		0.0f, -1.0f, 0.0f,	0.5f, -1.0f, 1.0f,		//bottomOutB
			0.5f, -1.0f, 1.0f,		0.0f, -1.0f, 0.0f,	1.0f, -1.0f, 0.5f,		//bottomOutBR
			1.0f, -1.0f, 0.5f,		0.0f, -1.0f, 0.0f,	1.0f, -1.0f, -0.5f,		//bottomOutR
			1.0f, -1.0f, -0.5f,		0.0f, -1.0f, 0.0f,	0.5f, -1.0f, -1.0f,		//bottomOutFR
			0.5f, -1.0f, -1.0f,		0.0f, -1.0f, 0.0f,	-0.5f, -1.0f, -1.0f,	//bottomOutF
			-0.5f, -1.0f, -1.0f,	0.0f, -1.0f, 0.0f,	-1.0f, -1.0f, -0.5f,	//bottomOutFL
			-1.0f, -1.0f, -0.5f,	0.0f, -1.0f, 0.0f,	-1.0f, -1.0f, 0.5f,		//bottomOutL
			-1.0f, -1.0f, 0.5f,		0.0f, -1.0f, 0.0f,	-0.5f, -1.0f, 1.0f,		//bottomOutBL
	  
			//bottom inside octagon
			-0.25f, -0.75f, 0.75f,		0.0f, -0.75f, 0.0f,		0.25f, -0.75f, 0.75f,	//topInB
			0.25f, -0.75f, 0.75f,		0.0f, -0.75f, 0.0f,		0.75f, -0.75f, 0.25f,	//topInBR
			0.75f, -0.75f, 0.25f,		0.0f, -0.75f, 0.0f,		0.75f, -0.75f, -0.25f,	//topInR
			0.75f, -0.75f, -0.25f,		0.0f, -0.75f, 0.0f,		0.25f, -0.75f, -0.75f,	//topInFR
			0.25f, -0.75f, -0.75f,		0.0f, -0.75f, 0.0f,		-0.25f, -0.75f, -0.75f, //topInF
			-0.25f, -0.75f, -0.75f,		0.0f, -0.75f, 0.0f,		-0.75f, -0.75f, -0.25f,	//topInFL
			-0.75f, -0.75f, -0.25f,		0.0f, -0.75f, 0.0f,		-0.75f, -0.75f, 0.25f,	//topInL
			-0.75f, -0.75f, 0.25f,		0.0f, -0.75f, 0.0f,		-0.25f, -0.75f, 0.75f,	//topInBL
	  
			//outside rectangles
			-0.5f, 1.0f, -1.0f,		0.5f, 1.0f, -1.0f,		-0.5f, -1.0f, -1.0f,	//outFT1
			0.5f, -1.0f, -1.0f,		-0.5f, -1.0f, -1.0f,	0.5f, 1.0f, -1.0f,		//outFT2
			0.5f, 1.0f, -1.0f,		1.0f, 1.0f, -0.5f,		0.5f, -1.0f, -1.0f,		//outFRT1
			1.0f, -1.0f, -0.5f,		0.5f, -1.0f, -1.0f,		1.0f, 1.0f, -0.5f,		//outFRT2
			1.0f, 1.0f, -0.5f,		1.0f, 1.0f, 0.5f,		1.0f, -1.0f, -0.5f,		//outRT1
			1.0f, -1.0f, 0.5f,		1.0f, -1.0f, -0.5f,		1.0f, 1.0f, 0.5f,		//outRT2
			1.0f, 1.0f, 0.5f,		0.5f, 1.0f, 1.0f,		1.0f, -1.0f, 0.5f,		//outBRT1
			0.5f, -1.0f, 1.0f,		1.0f, -1.0f, 0.5f,		0.5f, 1.0f, 1.0f,		//outBRT2
			0.5f, 1.0f, 1.0f,		-0.5f, 1.0f, 1.0f,		0.5f, -1.0f, 1.0f,		//outBT1
			-0.5f, -1.0f, 1.0f,		0.5f, -1.0f, 1.0f,		-0.5f, 1.0f, 1.0f,		//outBT2
			-0.5f, 1.0f, 1.0f,		-1.0f, 1.0f, 0.5f,		-0.5f, -1.0f, 1.0f,		//outBLT1
			-1.0f, -1.0f, 0.5f,		-0.5f, -1.0f, 1.0f,		-1.0f, 1.0f, 0.5f,		//outBLT2
			-1.0f, 1.0f, 0.5f,		-1.0f, 1.0f, -0.5f,		-1.0f, -1.0f, 0.5f,		//outLT1
			-1.0f, -1.0f, -0.5f,	-1.0f, -1.0f, 0.5f,		-1.0f, 1.0f, -0.5f,		//outLT2
			-1.0f, 1.0f, -0.5f,		-0.5f, 1.0f, -1.0f,		-1.0f, -1.0f, -0.5f,	//outFLT1
			-0.5f, -1.0f, -1.0f,	-1.0f, -1.0f, -0.5f,	-0.5f, 1.0f, -1.0f,		//outFLT2
	  
			//inside rectangles
			-0.25f, 1.0f, -0.75f,		0.25f, 1.0f, -0.75f,		-0.25f, -0.75f, -0.75f,		//inFT1
			0.25f, -0.75f, -0.75f,		-0.25f, -0.75f, -0.75f,		0.25f, 1.0f, -0.75f,		//inFT2
			0.25f, 1.0f, -0.75f,		0.75f, 1.0f, -0.25f,		0.25f, -0.75f, -0.75f,		//inFRT1
			0.75f, -0.75f, -0.25f,		0.25f, -0.75f, -0.75f,		0.75f, 1.0f, -0.25f,		//inFRT2
			0.75f, 1.0f, -0.25f,		0.75f, 1.0f, 0.25f,			0.75f, -0.75f, -0.25f,		//inRT1
			0.75f, -0.75f, 0.25f,		0.75f, -0.75f, -0.25f,		0.75f, 1.0f, 0.25f,			//inRT2
			0.75f, 1.0f, 0.25f,			0.25f, 1.0f, 0.75f,			0.75f, -0.75f, 0.25f,		//inBRT1
			0.25f, -0.75f, 0.75f,		0.75f, -0.75f, 0.25f,		0.25f, 1.0f, 0.75f,			//inBRT2
			0.25f, 1.0f, 0.75f,			-0.25f, 1.0f, 0.75f,		0.25f, -0.75f, 0.75f,		//inBT1
			-0.25f, -0.75f, 0.75f,		0.25f, -0.75f, 0.75f,		-0.25f, 1.0f, 0.75f,		//inBT2
			-0.25f, 1.0f, 0.75f,		-0.75f, 1.0f, 0.25f,		-0.25f, -0.75f, 0.75f,		//inBLT1
			-0.75f, -0.75f, 0.25f,		-0.25f, -0.75f, 0.75f,		-0.75f, 1.0f, 0.25f,		//inBLT2
			-0.75f, 1.0f, 0.25f,		-0.75f, 1.0f, -0.25f,		-0.75f, -0.75f, 0.25f,		//inLT1
			-0.75f, -0.75f, -0.25f,		-0.75f, -0.75f, 0.25f,		-0.75f, 1.0f, -0.25f,		//inLT2
			-0.75f, 1.0f, -0.25f,		-0.25f, 1.0f, -0.75f,		-0.75f, -0.75f, -0.25f,		//inFLT1
			-0.25f, -0.75f, -0.75f,		-0.75f, -0.75f, -0.25f,		-0.25f, 1.0f, -0.75f,		//inFLT2
	  
			//top octagon ring
			-0.5f, 1.0f, 1.0f,			-0.25f, 1.0f, 0.75f,	0.5f, 1.0f, 1.0f,		//BT1
			-0.25f, 1.0f, 0.75f,		0.25f, 1.0f, 0.75f,		0.5f, 1.0f, 1.0f,		//BT2
			0.5f, 1.0f, 1.0f,			0.25f, 1.0f, 0.75f,		1.0f, 1.0f, 0.5f,		//BRT1
			0.25f, 1.0f, 0.75f,			1.0f, 1.0f, 0.5f,		0.75f, 1.0f, 0.25f,		//BRT2
			1.0f, 1.0f, 0.5f,			0.75f, 1.0f, 0.25f,		1.0f, 1.0f, -0.5f,		//RT1
			0.75f, 1.0f, 0.25f,			0.75f, 1.0f, -0.25f,	1.0f, 1.0f, -0.5f,		//RT2
			1.0f, 1.0f, -0.5f,			0.75f, 1.0f, -0.25f,	0.5f, 1.0f, -1.0f,		//FRT1
			0.75f, 1.0f, -0.25f,		0.5f, 1.0f, -1.0f,		0.25f, 1.0f, -0.75f,	//FRT2
			0.25f, 1.0f, -0.75f,		0.5f, 1.0f, -1.0f,		-0.5f, 1.0f, -1.0f,		//FT1
			0.25f, 1.0f, -0.75f,		-0.5f, 1.0f, -1.0f,		-0.25f, 1.0f, -0.75f,	//FT2
			-0.25f, 1.0f, -0.75f,		-0.5f, 1.0f, -1.0f,		-1.0f, 1.0f, -0.5f,		//FLT1
			-0.25f, 1.0f, -0.75f,		-1.0f, 1.0f, -0.5f,		-0.75f, 1.0f, -0.25f,	//FLT2
			-0.75f, 1.0f, -0.25f,		-1.0f, 1.0f, -0.5f,		-1.0f, 1.0f, 0.5f,		//LT1
			-0.75f, 1.0f, -0.25f,		-1.0f, 1.0f, 0.5f,		-0.75f, 1.0f, 0.25f,	//LT2
			-0.75f, 1.0f, 0.25f,		-1.0f, 1.0f, 0.5f,		-0.5f, 1.0f, 1.0f,		//BLT1
			-0.75f, 1.0f, 0.25f,		-0.5f, 1.0f, 1.0f,		-0.25f, 1.0f, 0.75f,	//BLT2
		};
		float[] octTexcoords = new float[]
		{
			//bottom outside octagon
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
	  
			//bottom inside octagon
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
	  
			//outside rectangles
			0.5f, 1.0f,		1.0f, 0.0f,		0.0f, 0.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.5f, 1.0f,		1.0f, 0.0f,		0.0f, 0.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.5f, 1.0f,		1.0f, 0.0f,		0.0f, 0.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.5f, 1.0f,		1.0f, 0.0f,		0.0f, 0.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.5f, 1.0f,		1.0f, 0.0f,		0.0f, 0.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.5f, 1.0f,		1.0f, 0.0f,		0.0f, 0.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.5f, 1.0f,		1.0f, 0.0f,		0.0f, 0.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.5f, 1.0f,		1.0f, 0.0f,		0.0f, 0.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
	  
			//inside rectangles
			0.5f, 1.0f,		1.0f, 0.0f,		0.0f, 0.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.5f, 1.0f,		1.0f, 0.0f,		0.0f, 0.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.5f, 1.0f,		1.0f, 0.0f,		0.0f, 0.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.5f, 1.0f,		1.0f, 0.0f,		0.0f, 0.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.5f, 1.0f,		1.0f, 0.0f,		0.0f, 0.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.5f, 1.0f,		1.0f, 0.0f,		0.0f, 0.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.5f, 1.0f,		1.0f, 0.0f,		0.0f, 0.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.5f, 1.0f,		1.0f, 0.0f,		0.0f, 0.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
	  
			//top octagon ring
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
			0.0f, 0.0f,		1.0f, 0.0f,		0.5f, 1.0f,
		};
		
		// xyz axes vertex coordinates
		float[] xyzAxesPos = new float[]
		{
			0.0f, 0.0f, 0.0f,	3.0f, 0.0f, 0.0f,	// x axis
			0.0f, 0.0f, 0.0f,	0.0f, 3.0f, 0.0f,	// y axis
			0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 3.0f	// z axis
		};
		
		
		// increase texture coordinate range to have some coordinates outside (0, 1) for tiling
		for(int i = 0; i < octTexcoords.length; i++) {
			octTexcoords[i] = octTexcoords[i] * 2;
		} // end for
		for(int i = 0; i < cubeTexCoords.length; i++) {
			cubeTexCoords[i] = cubeTexCoords[i] * 2;
		} // end for
		
		
		// complex cube obj file imported object (code from chapter 6 - program 3)
		numObjVertices = myModel.getNumVertices();
		Vector3f[] vertices = myModel.getVertices();
		Vector2f[] texCoords = myModel.getTexCoords();
		Vector3f[] normals = myModel.getNormals();
		float[] pvalues = new float[numObjVertices*3];
		float[] tvalues = new float[numObjVertices*2];
		float[] nvalues = new float[numObjVertices*3];
		for (int i=0; i<numObjVertices; i++)
		{	pvalues[i*3]   = (float) (vertices[i]).x();
			pvalues[i*3+1] = (float) (vertices[i]).y();
			pvalues[i*3+2] = (float) (vertices[i]).z();
			tvalues[i*2]   = (float) (texCoords[i]).x();
			tvalues[i*2+1] = (float) (texCoords[i]).y();
			nvalues[i*3]   = (float) (normals[i]).x();
			nvalues[i*3+1] = (float) (normals[i]).y();
			nvalues[i*3+2] = (float) (normals[i]).z();
		}
		
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);
		
		// cube
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer cubeBuf = Buffers.newDirectFloatBuffer(cubePositions);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeBuf.limit()*4, cubeBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer cubeTexBuf = Buffers.newDirectFloatBuffer(cubeTexCoords);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeTexBuf.limit()*4, cubeTexBuf, GL_STATIC_DRAW);
		
		// pyramid
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer pyrBuf = Buffers.newDirectFloatBuffer(pyramidPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, pyrBuf.limit()*4, pyrBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer pyrTextBuf = Buffers.newDirectFloatBuffer(pyrTextureCoordinates);
		gl.glBufferData(GL_ARRAY_BUFFER, pyrTextBuf.limit()*4, pyrTextBuf, GL_STATIC_DRAW);
		
		// octagon manual object
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer octBuf = Buffers.newDirectFloatBuffer(octPos);
		gl.glBufferData(GL_ARRAY_BUFFER, octBuf.limit()*4, octBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		FloatBuffer octTexBuf = Buffers.newDirectFloatBuffer(octTexcoords);
		gl.glBufferData(GL_ARRAY_BUFFER, octTexBuf.limit()*4, octTexBuf, GL_STATIC_DRAW);
		
		// complex cube imported obj file object
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		FloatBuffer complexCubeBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, complexCubeBuf.limit()*4, complexCubeBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		FloatBuffer complexCubeTexBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, complexCubeTexBuf.limit()*4, complexCubeTexBuf, GL_STATIC_DRAW);
		
		// xyz axes
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		FloatBuffer xyzBuf = Buffers.newDirectFloatBuffer(xyzAxesPos);
		gl.glBufferData(GL_ARRAY_BUFFER, xyzBuf.limit()*4, xyzBuf, GL_STATIC_DRAW);
	}

	public static void main(String[] args) { new Code(); }
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
	public void dispose(GLAutoDrawable drawable) {}
}
package a4;

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

public class Code extends JFrame implements GLEventListener, KeyListener, MouseWheelListener
{	private GLCanvas myCanvas;
	private int renderingProgram0, renderingProgram, axesProgram, cubeMapProgram, terrainProgram;
	private int vao[] = new int[1];
	private int vbo[] = new int[17];
	private float cameraX, cameraY, cameraZ;
	private float cubeLocX, cubeLocY, cubeLocZ;
	private float pyrLocX, pyrLocY, pyrLocZ;
	private float octLocX, octLocY, octLocZ;
	private float impLocX, impLocY, impLocZ;
	
	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4fStack mvStack = new Matrix4fStack(4);
	private Matrix4f pMat = new Matrix4f();  	// perspective matrix
	private Matrix4f vMat = new Matrix4f();  	// view matrix
	private Matrix4f mMat = new Matrix4f();  	// model matrix
	private Matrix4f mvMat = new Matrix4f(); 	// model-view matrix
	private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose matrix
	private int mLoc, vLoc, nLoc, sLoc;
	private int mvLoc, pLoc, mvLocAxes, pLocAxes;
	private float aspect;
	
	private int skyboxTexture;
	
	// Variables for movement based on elapsed time.
	private double timeFactor;
	private double startTime;
	private double elapsedTime;
	
	private int numObjVertices;
	private ImportedModel myModel, sphere;
	
	private int cubeTexture, pyramidTexture, octTexture, impTexture;
	
	private boolean axesVisible = true;
	
	private Camera camera;
		
	private float xInc = 0, yInc = 0, zInc = 0;
	private boolean xEnable = false, yEnable = false, zEnable = false;
	
	// light
	private Vector3f currentLightPos = new Vector3f();
	private Vector3f initialLightLoc = new Vector3f(5.0f, 5.0f, 5.0f);
	private float[] lightPos = new float[3];
	private int globalAmbLoc, ambLoc, diffLoc, specLoc, posLoc, mambLoc, mdiffLoc, mspecLoc, mshiLoc;
	private int toggleLight = 1, toggleLightLoc;

	// white light properties
	float[] globalAmbient = new float[] { 0.6f, 0.6f, 0.6f, 1.0f };
	float[] lightAmbient = new float[] { 0.1f, 0.1f, 0.1f, 1.0f };
	float[] lightDiffuse = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	float[] lightSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		
	// material properties
	float[] matAmb = Utils.goldAmbient();
	float[] matDif = Utils.goldDiffuse();
	float[] matSpe = Utils.goldSpecular();
	float matShi = Utils.goldShininess();
	
	// shadow
	private int scSizeX, scSizeY;
	private int [] shadowTex = new int[1];
	private int [] shadowBuffer = new int[1];
	private Matrix4f lightVmat = new Matrix4f();
	private Matrix4f lightPmat = new Matrix4f();
	private Matrix4f shadowMVP1 = new Matrix4f();
	private Matrix4f shadowMVP2 = new Matrix4f();
	private Matrix4f b = new Matrix4f();
	
	private Vector3f origin = new Vector3f(0.0f, 0.0f, 0.0f);
	private Vector3f originUp = new Vector3f(0.0f, 1.0f, 0.0f);
	
	//
	private Vector3f terLoc = new Vector3f(0.0f, 0.0f, 0.0f);
	private float tessInner = 30.0f;
	private float tessOuter = 20.0f;
	private int moonTex, moonHeight, moonNormal;

	public Code()
	{	setTitle("Assignment #4");
		setSize(600, 600);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		this.add(myCanvas);
		this.setVisible(true);
		myCanvas.addKeyListener(this);
		myCanvas.addMouseWheelListener(this);
		
		Animator animator = new Animator(myCanvas);
		animator.start();
	} // end Code constructor

	public void display(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		
		// Movement based on elapsed time.
		elapsedTime = System.currentTimeMillis() - startTime;
		timeFactor = elapsedTime / 1000.0;
		
		// draw cube map
		drawCubeMap(gl);
		
		// draw objects with shadows
			// set light position
		currentLightPos.set(initialLightLoc);
		currentLightPos.add(new Vector3f(xInc, yInc, zInc));
			// build lightVmat
		lightVmat.identity();
		lightVmat.setLookAt(currentLightPos, origin, originUp);
			//build lightPmat
		lightPmat.identity();
		lightPmat.setPerspective((float)Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);
			// bind shadowBuffer
		gl.glBindFramebuffer(GL_FRAMEBUFFER, shadowBuffer[0]);
		gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadowTex[0], 0);
			// reduce artifacts
		gl.glDrawBuffer(GL_NONE);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_POLYGON_OFFSET_FILL); // reducing shadow artifacts
		gl.glPolygonOffset(3.0f, 5.0f);		 // reducing shadow artifacts

		// TODO: change scene to be coherent (ground plane (with asphalt texture??), car model, 
		//		 cone model (from online?? or permission form gary??), third model from NASA???

			// pass one for shadows
		passOne(gl);
			// reducing shadow artifacts
		gl.glDisable(GL_POLYGON_OFFSET_FILL);
			// bind shadows for fragment shader
		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);
		
		gl.glDrawBuffer(GL_FRONT);
		
			// pass two for shadows
		passTwo(gl);

		//vMat.identity();
		//vMat.translation(-cameraX,-cameraY,-cameraZ);
		//vMat = camera.getViewMatrix();
	
		// draw other objects
		//drawOtherObjects(gl);
		
		// draw xyz axes
		drawXYZAxes(gl);
		
		// draw terrain
		drawTerrain(gl);
	} // end display
	
	// pass one for shadows
	private void passOne(GL4 gl) {
		gl.glUseProgram(renderingProgram0);
		
		// imported object
			// apply transforms to mMat
		mMat.identity();
		mMat.translate(impLocX, impLocY + (float)Math.cos(timeFactor)*(float)Math.sin(timeFactor)*5.0f, impLocZ);
		mMat.rotateXYZ(0.75f*(float)timeFactor, 0.0f, 0.75f*(float)timeFactor);
			// build shadowMVP1
		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);
			// associate sLoc with renderingProgram0
		sLoc = gl.glGetUniformLocation(renderingProgram0, "shadowMVP");
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));
			// draw vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, myModel.getNumVertices());
		
		// pyramid
			// apply transforms to mMat
		mMat.identity();
		//mMat.translate(pyrLocX, pyrLocY, pyrLocZ);
		mMat.translate((float)Math.sin(timeFactor)*5.0f, (float)Math.cos(timeFactor)*(float)Math.sin(timeFactor)*5.0f + 2.0f, -4.5f);
			// build shadowMVP1
		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);
			// associate sLoc with renderingProgram0
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));
			// draw vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 18);
		
		// cube
			// apply transforms to mMat
		mMat.identity();
		mMat.translation(cubeLocX, cubeLocY, cubeLocZ);
			// build shadowMVP1
		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);
			// associate sLoc with renderingProgram0
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));
			// draw vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		
		// manual octagon object
			// apply transforms to mMat
		mMat.identity();
		mMat.translation(octLocX, octLocY, octLocZ);
			// build shadowMVP1
		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);
			// associate sLoc with renderingProgram0
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));
			// draw vertices
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 192);
	} // end passOne
	
	// pass two for shadows
	private void passTwo(GL4 gl) {		
		// use renderingProgram
		gl.glUseProgram(renderingProgram);
		
		// associate with renderingProgram
		mvLoc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
		mLoc = gl.glGetUniformLocation(renderingProgram, "m_matrix");
		vLoc = gl.glGetUniformLocation(renderingProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgram, "p_matrix");	
		nLoc = gl.glGetUniformLocation(renderingProgram, "norm_matrix");
		sLoc = gl.glGetUniformLocation(renderingProgram, "shadowMVP");
		
		// build vMat
		vMat.identity();
		vMat.setTranslation(-cameraX, -cameraY, -cameraZ);
		vMat = camera.getViewMatrix();
		
		// imported object
			// assign light values
		matAmb = Utils.silverAmbient();
		matDif = Utils.silverDiffuse();
		matSpe = Utils.silverSpecular();
		matShi = Utils.silverShininess();
		installLights();
			// apply transforms to mMat
		mMat.identity();
		mMat.translate(impLocX, impLocY + (float)Math.cos(timeFactor)*(float)Math.sin(timeFactor)*5.0f, impLocZ);
		mMat.rotateXYZ(0.75f*(float)timeFactor, 0.0f, 0.75f*(float)timeFactor);
			// assign invTrMat
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);
			// build shadowMVP2
		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);
			// build mvMat
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
			// associate with renderingProgram
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
			// vertices for imported object
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
			// texture for imported object
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, impTexture);
			// normals for imported object
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
			// draw imported object
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, myModel.getNumVertices());
			
		// pyramid
			// assign light values
		matAmb = Utils.bronzeAmbient();
		matDif = Utils.bronzeDiffuse();
		matSpe = Utils.bronzeSpecular();
		matShi = Utils.bronzeShininess();
		installLights();
			// apply transforms to mMat
		mMat.identity();
		//mMat.translate(pyrLocX, pyrLocY, pyrLocZ);
		mMat.translate((float)Math.sin(timeFactor)*5.0f, (float)Math.cos(timeFactor)*(float)Math.sin(timeFactor)*5.0f + 2.0f, -4.5f);
			// assign invTrMat
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);
			// build shadowMVP2
		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);
			// build mvMat
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
			// associate with renderingProgram
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
			// vertices for pyramid
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
			// texture for pyramid
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, pyramidTexture);
			// normals for pyramid
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
			// draw pyramid
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 18);

		// cube
			// assign light values
		matAmb = Utils.chromeAmbient();
		matDif = Utils.chromeDiffuse();
		matSpe = Utils.chromeSpecular();
		matShi = Utils.chromeShininess();
		installLights();
			// apply transforms to mMat
		mMat.identity();
		mMat.translation(cubeLocX, cubeLocY, cubeLocZ);
			// assign invTrMat
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);
			// build shadowMVP2
		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);
			// build mvMat
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
			// associate with renderingProgram
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
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
			// texture tiling
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_MIRRORED_REPEAT);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_MIRRORED_REPEAT);		
			// normals for cube
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
			// draw cube
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		
		// manual octagon object
			// assign light values
		matAmb = Utils.goldAmbient();
		matDif = Utils.goldDiffuse();
		matSpe = Utils.goldSpecular();
		matShi = Utils.goldShininess();
		installLights();
			// apply transforms to mMat
		mMat.identity();
		mMat.translation(octLocX, octLocY, octLocZ);
			// assign invTrMat
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);
			// build shadowMVP2
		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);
			// build mvMat
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
			// associate with renderingProgram
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
			// vertices for manual octagon object
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
			// texture for manual octagon object
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, octTexture);
			// texture tiling
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
			// normals for octagon object
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
			// draw manual octagon object
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 192);
		
		// sphere for light
			// assign yellow color light values
		matAmb = new float[] {1.0f, 	0.964706f, 	0.0f, 		1.0f};
		matDif = new float[] {1.0f, 	0.964706f, 	0.0f, 		1.0f};
		matSpe = new float[] {0.0225f,	0.0225f,	0.0225f, 	1.0f};
		matShi = 12.8f;
		installLights();
			// apply transforms to mMat
		mMat.identity();
		mMat.translation(currentLightPos.x(), currentLightPos.y(), currentLightPos.z());
			// assign invTrMat
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);
			// build shadowMVP2
		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);
			// build mvMat
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
			// associate with renderingProgram
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
			// vertices for sphere
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[14]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
			// texture for sphere
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[15]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, octTexture);
			// normals for sphere
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[16]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
			// draw sphere
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, sphere.getNumVertices());
	} // end passTwo
	
	// draw cube map
	private void drawCubeMap(GL4 gl) {
		gl.glUseProgram(cubeMapProgram);
		vLoc = gl.glGetUniformLocation(cubeMapProgram, "v_matrix");
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		pLoc = gl.glGetUniformLocation(cubeMapProgram, "p_matrix");
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[13]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, skyboxTexture);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDisable(GL_CULL_FACE);
	} // end drawCubeMap
	
	/*
	// draw other objects
	private void drawOtherObjects(GL4 gl) {
		gl.glUseProgram(renderingProgram);
		mvLoc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
		mLoc = gl.glGetUniformLocation(renderingProgram, "m_matrix");
		vLoc = gl.glGetUniformLocation(renderingProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgram, "p_matrix");	
		nLoc = gl.glGetUniformLocation(renderingProgram, "norm_matrix");		

		mMat.translate(0, 0, 0);

		currentLightPos.set(initialLightLoc);
		currentLightPos.add(new Vector3f(xInc, yInc, zInc));
				
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);
		
		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));

		drawShapes(gl, timeFactor);
	} // end drawOtherObjects
	*/
	
	// draw xyz axes
	private void drawXYZAxes(GL4 gl) {
		//gl.glUseProgram(axesProgram);
		mvLocAxes = gl.glGetUniformLocation(axesProgram, "mv_Matrix");
		pLocAxes = gl.glGetUniformLocation(axesProgram, "p_Matrix");
		mMat.translation(0.0f, 0.0f, 0.0f);
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[12]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		if(axesVisible) {
			// values obtained from https://globe3d.sourceforge.io/g3d_html/gl-materials__ads.htm
			
			// red
			matAmb = new float[] {1,		0,			0,			1};
			matDif = new float[] {1,		0,			0,			1};
			matSpe = new float[] {0.0225f,	0.0225f,	0.0225f,	1};
			matShi = 12.8f;
			installLights();
			gl.glDrawArrays(GL_LINES, 0, 2);
			
			// green
			matAmb = new float[] {0,		1,			0,			1};
			matDif = new float[] {0,		1,			0,			1};
			matSpe = new float[] {0.0225f,	0.0225f,	0.0225f,	1};
			matShi = 12.8f;
			installLights();
			gl.glDrawArrays(GL_LINES, 2, 2);
			
			// blue
			matAmb = new float[] {0,		0,			1,			1};
			matDif = new float[] {0,		0,			1,			1};
			matSpe = new float[] {0.0225f,	0.0225f,	0.0225f,	1};
			matShi = 12.8f;
			installLights();
			gl.glDrawArrays(GL_LINES, 4, 2);
		} // end if
	} // end drawXYZAxes
	
	// draw height and normal mapped terrain
	private void drawTerrain(GL4 gl) {
		// use terrainProgram
		gl.glUseProgram(terrainProgram);
			// associate with renderingProgram
		//mvLoc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
		mLoc = gl.glGetUniformLocation(terrainProgram, "m_matrix");
		vLoc = gl.glGetUniformLocation(terrainProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(terrainProgram, "p_matrix");	
		nLoc = gl.glGetUniformLocation(terrainProgram, "norm_matrix");
		//sLoc = gl.glGetUniformLocation(renderingProgram, "shadowMVP");
			// build vMat
		vMat.identity();
		vMat.setTranslation(-cameraX, -cameraY, -cameraZ);
		vMat = camera.getViewMatrix();
		// terrain
			// apply transforms to mMat
		mMat.identity();
		mMat.setTranslation(terLoc.x(), terLoc.y() - 10, terLoc.z());
		//mMat.rotateX((float)20.0f);
		mMat.scale(100.0f, 25.0f, 100.0f);
			// assign invTrMat
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);
			// install lights
		installLights();
			// associate with renderingProgram
		//gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		//gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
			// texture
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, moonTex);
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, moonHeight);
		gl.glActiveTexture(GL_TEXTURE2);
		gl.glBindTexture(GL_TEXTURE_2D, moonNormal);
			// draw terrain
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CW);
		gl.glPatchParameteri(GL_PATCH_VERTICES, 4);
		gl.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
		gl.glDrawArraysInstanced(GL_PATCHES, 0, 4, 64*64);
	} // end drawTerrain
	
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
			case KeyEvent.VK_O:
				toggleLight++;
				break;
			case KeyEvent.VK_X:
				xEnable = !xEnable;
				break;
			case KeyEvent.VK_Y:
				yEnable = !yEnable;
				break;
			case KeyEvent.VK_Z:
				zEnable = !zEnable;
				break;
		} // end switch
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
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		if(xEnable) {
			if(e.getWheelRotation() > 0) {
				xInc += -1.0f;
			} // end if
			else if(e.getWheelRotation() < 0){
				xInc += 1.0f;
			} // end else if
		} // end if
		if(yEnable) {
			if(e.getWheelRotation() > 0) {
				yInc += -1.0f;
			} // end if
			else if(e.getWheelRotation() < 0){
				yInc += 1.0f;
			} // end else if
		} // end if
		if(zEnable) {
			if(e.getWheelRotation() > 0) {
				zInc += -1.0f;
			} // end if
			else if(e.getWheelRotation() < 0){
				zInc += 1.0f;
			} // end else if
		} // end if
	} // end mouseWheelMoved
	
	private void drawShapes(GL4 gl, double timeFactor) {
			// push the view matrix to the stack
		mvStack.pushMatrix();
		mvStack.mul(vMat);
		
		
		matAmb = Utils.silverAmbient();
		matDif = Utils.silverDiffuse();
		matSpe = Utils.silverSpecular();
		matShi = Utils.silverShininess();
		installLights();
		
		// draw the imported object using buffer #9, texture using buffer #10, normals using buffer #11
			// push translate and rotate transforms to the stack
		mvStack.pushMatrix();
		mvStack.translate(impLocX, impLocY + (float)Math.cos(timeFactor)*(float)Math.sin(timeFactor)*5.0f, impLocZ);
		mvStack.pushMatrix();
		mvStack.rotateXYZ(0.75f*(float)timeFactor, 0.0f, 0.75f*(float)timeFactor);
			// vertices for imported object
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
			// texture for imported object
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, impTexture);
			// normals for imported object
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
			// draw imported object
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, myModel.getNumVertices());
			
			// pop rotate transform from the stack
		mvStack.popMatrix();
		
		
		matAmb = Utils.bronzeAmbient();
		matDif = Utils.bronzeDiffuse();
		matSpe = Utils.bronzeSpecular();
		matShi = Utils.bronzeShininess();
		installLights();
		
		// draw the pyramid using buffer #3, texture using buffer #4, normals using buffer #5
			// push translate transform to the stack
		mvStack.pushMatrix();
		mvStack.translate((float)Math.sin(timeFactor)*5.0f, (float)Math.cos(timeFactor)*(float)Math.sin(timeFactor)*5.0f, 4.5f);
			// vertices for pyramid
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
			// texture for pyramid
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, pyramidTexture);
			// normals for pyramid
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
			// draw pyramid
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 18);
			
			// pop contents of the stack
		mvStack.popMatrix();
		mvStack.popMatrix();
		mvStack.popMatrix();
		
		
		matAmb = Utils.chromeAmbient();
		matDif = Utils.chromeDiffuse();
		matSpe = Utils.chromeSpecular();
		matShi = Utils.chromeShininess();
		installLights();

		// draw the cube using buffer #0, texture using buffer #1, normals using buffer #2
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
			// texture tiling
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_MIRRORED_REPEAT);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_MIRRORED_REPEAT);		
			// normals for cube
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
			// draw cube
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
			

		matAmb = Utils.goldAmbient();
		matDif = Utils.goldDiffuse();
		matSpe = Utils.goldSpecular();
		matShi = Utils.goldShininess();
		installLights();
		
		// draw the manual octagon object using buffer #6, texture using buffer #7, normals using buffer #8
		mMat.translation(octLocX, octLocY, octLocZ);
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
			// vertices for manual octagon object
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
			// texture for manual octagon object
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, octTexture);
			// texture tiling
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
			// normals for octagon object
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
			// draw manual octagon object
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 192);
		
		
		// yellow
		matAmb = new float[] {1.0f, 	0.964706f, 	0.0f, 		1.0f};
		matDif = new float[] {1.0f, 	0.964706f, 	0.0f, 		1.0f};
		matSpe = new float[] {0.0225f,	0.0225f,	0.0225f, 	1.0f};
		matShi = 12.8f;
		installLights();
		
		// draw the imported sphere object using buffer # 14, tecture using buffer #15, and normals using buffer #16
		mMat.translation(currentLightPos.x(), currentLightPos.y(), currentLightPos.z());
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
			// vertices for sphere
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[14]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
			// texture for sphere
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[15]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, octTexture);
			// normals for sphere
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[16]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
			// draw sphere
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, sphere.getNumVertices());
	} // end drawShapes

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		startTime = System.currentTimeMillis();
	
		cubeMapProgram = Utils.createShaderProgram("a4/cmVertShader.glsl", "a4/cmFragShader.glsl");
		renderingProgram0 = Utils.createShaderProgram("a4/vert0Shader.glsl", "a4/frag0Shader.glsl");
		renderingProgram = Utils.createShaderProgram("a4/vertShader.glsl", "a4/fragShader.glsl");
		terrainProgram = Utils.createShaderProgram("a4/terrVertShader.glsl", "a4/tessCShader.glsl", "a4/tessEShader.glsl", "a4/terrFragShader.glsl");
		axesProgram = Utils.createShaderProgram("a4/axesVertShader.glsl", "a4/axesFragShader.glsl");

		moonTex = Utils.loadTexture("squareMoonMap.jpg");
		moonHeight = Utils.loadTexture("squareMoonBump.jpg");
		moonNormal = Utils.loadTexture("squareMoonNormal.jpg");
		
		myModel = new ImportedModel("complexCube.obj");
		sphere = new ImportedModel("sphere.obj");
		
		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);
		
		setupVertices();
		setupShadowBuffer();
		
		b.set(0.5f, 0.0f, 0.0f, 0.0f,
			  0.0f, 0.5f, 0.0f, 0.0f,
			  0.0f, 0.0f, 0.5f, 0.0f,
			  0.5f, 0.5f, 0.5f, 1.0f);
		
		cameraX = 0.0f; cameraY = 0.0f; cameraZ = 8.0f;
		cubeLocX = -3.0f; cubeLocY = -3.0f; cubeLocZ = 0.0f;
		pyrLocX = 2.0f; pyrLocY = 2.0f; pyrLocZ = -3.0f;
		octLocX = 3.0f; octLocY = -3.0f; octLocZ = 0.0f;
		impLocX = 0.0f;	impLocY = 2.0f;	impLocZ = -10.0f;
				
		cubeTexture = Utils.loadTexture("customTexture.png");
		pyramidTexture = Utils.loadTexture("brick1.jpg");
		octTexture = Utils.loadTexture("wrinkled-page.jpg");
		impTexture = Utils.loadTexture("ice.jpg");
		
		skyboxTexture = Utils.loadCubeMap("cubeMap");
		gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
		
		camera = new Camera();
		camera.setCamLocation(new Vector3f(cameraX, cameraY, cameraZ));
	} // end init
	
	private void setupShadowBuffer() {
		GL4 gl = (GL4)GLContext.getCurrentGL();
		scSizeX = myCanvas.getWidth();
		scSizeY = myCanvas.getHeight();
		
		gl.glGenFramebuffers(1, shadowBuffer, 0);
		
		gl.glGenTextures(1, shadowTex, 0);
		gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32, scSizeX, 
						scSizeY, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
		
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	} // end setupShadowBuffer

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
		float[] cubeNormals = new float[]
		{ 
			0.0f, 0.0f, -1.0f,  0.0f, 0.0f, -1.0f,  0.0f, 0.0f, -1.0f, 	// back face
			0.0f, 0.0f, -1.0f,  0.0f, 0.0f, -1.0f,  0.0f, 0.0f, -1.0f,
			1.0f, 0.0f, 0.0f,  	1.0f, 0.0f, 0.0f,  	1.0f, 0.0f, 0.0f, 	// right face
			1.0f, 0.0f, 0.0f,  	1.0f, 0.0f, 0.0f,  	1.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 1.0f,  	0.0f, 0.0f, 1.0f,  	0.0f, 0.0f, 1.0f, 	// front face
			0.0f, 0.0f, 1.0f,  	0.0f, 0.0f, 1.0f,  	0.0f, 0.0f, 1.0f,
			-1.0f, 0.0f, 0.0f,  -1.0f, 0.0f, 0.0f,  -1.0f, 0.0f, 0.0f, 	// left face
			-1.0f, 0.0f, 0.0f,  -1.0f, 0.0f, 0.0f,  -1.0f, 0.0f, 0.0f,
			0.0f, -1.0f, 0.0f,  0.0f, -1.0f, 0.0f,  0.0f, -1.0f, 0.0f, 	// bottom face
			0.0f, -1.0f, 0.0f,  0.0f, -1.0f, 0.0f,  0.0f, -1.0f, 0.0f,
			0.0f, 1.0f, 0.0f,  	0.0f, 1.0f, 0.0f,  	0.0f, 1.0f, 0.0f, 	// top face
			0.0f, 1.0f, 0.0f,  	0.0f, 1.0f, 0.0f,  	0.0f, 1.0f, 0.0f 
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
		float[] pyrNormals = 
		{
			0.0f, 		-1.0f, 		0.0f,
			0.894427f, 	0.447214f, 	0.0f,
			-0.0f, 		0.447214f, 	0.894427f,
			-0.894427f, 0.447214f, 	-0.0f,
			0.0f, 		0.447214f, 	-0.894427f
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
		float[] octNormals = new float[]
		{
			//bottom outside octagon
			0.0f, -2.0f, 0.0f,	0.0f, -2.0f, 0.0f,	0.0f, -2.0f, 0.0f,
			0.0f, -2.0f, 0.0f,	0.0f, -2.0f, 0.0f,	0.0f, -2.0f, 0.0f,
			0.0f, -2.0f, 0.0f,	0.0f, -2.0f, 0.0f,	0.0f, -2.0f, 0.0f,
			0.0f, -2.0f, 0.0f,	0.0f, -2.0f, 0.0f,	0.0f, -2.0f, 0.0f,
			0.0f, -2.0f, 0.0f,	0.0f, -2.0f, 0.0f,	0.0f, -2.0f, 0.0f,
			0.0f, -2.0f, 0.0f,	0.0f, -2.0f, 0.0f,	0.0f, -2.0f, 0.0f,
			0.0f, -2.0f, 0.0f,	0.0f, -2.0f, 0.0f,	0.0f, -2.0f, 0.0f,
			0.0f, -2.0f, 0.0f,	0.0f, -2.0f, 0.0f,	0.0f, -2.0f, 0.0f,
	  
			//bottom inside octagon
			0.0f, 0.25f, 0.0f,	0.0f, 0.25f, 0.0f,	0.0f, 0.25f, 0.0f,
			0.0f, 0.25f, 0.0f,	0.0f, 0.25f, 0.0f,	0.0f, 0.25f, 0.0f,
			0.0f, 0.25f, 0.0f,	0.0f, 0.25f, 0.0f,	0.0f, 0.25f, 0.0f,
			0.0f, 0.25f, 0.0f,	0.0f, 0.25f, 0.0f,	0.0f, 0.25f, 0.0f,
			0.0f, 0.25f, 0.0f,	0.0f, 0.25f, 0.0f,	0.0f, 0.25f, 0.0f,
			0.0f, 0.25f, 0.0f,	0.0f, 0.25f, 0.0f,	0.0f, 0.25f, 0.0f,
			0.0f, 0.25f, 0.0f,	0.0f, 0.25f, 0.0f,	0.0f, 0.25f, 0.0f,
			0.0f, 0.25f, 0.0f,	0.0f, 0.25f, 0.0f,	0.0f, 0.25f, 0.0f,
	  
			//outside rectangles
				//Front
			0.0f, 0.0f, -2.0f,	0.0f, 0.0f, -2.0f,	0.0f, 0.0f, -2.0f,
			0.0f, 0.0f, -2.0f,	0.0f, 0.0f, -2.0f,	0.0f, 0.0f, -2.0f,
				//FrontRight
			2.0f, 0.0f, -2.0f,	2.0f, 0.0f, -2.0f,	2.0f, 0.0f, -2.0f,
			2.0f, 0.0f, -2.0f,	2.0f, 0.0f, -2.0f,	2.0f, 0.0f, -2.0f,
				//Right
			2.0f, 0.0f, 0.0f,	2.0f, 0.0f, 0.0f,	2.0f, 0.0f, 0.0f,
			2.0f, 0.0f, 0.0f,	2.0f, 0.0f, 0.0f,	2.0f, 0.0f, 0.0f,
				//BackRight
			2.0f, 0.0f, 2.0f,	2.0f, 0.0f, 2.0f,	2.0f, 0.0f, 2.0f,
			2.0f, 0.0f, 2.0f,	2.0f, 0.0f, 2.0f,	2.0f, 0.0f, 2.0f,
				//Back
			0.0f, 0.0f, 2.0f,	0.0f, 0.0f, 2.0f,	0.0f, 0.0f, 2.0f,
			0.0f, 0.0f, 2.0f,	0.0f, 0.0f, 2.0f,	0.0f, 0.0f, 2.0f,
				//BackLeft
			-2.0f, 0.0f, 2.0f,	-2.0f, 0.0f, 2.0f,	-2.0f, 0.0f, 2.0f,
			-2.0f, 0.0f, 2.0f,	-2.0f, 0.0f, 2.0f,	-2.0f, 0.0f, 2.0f,
				//Left
			-2.0f, 0.0f, 0.0f,	-2.0f, 0.0f, 0.0f,	-2.0f, 0.0f, 0.0f,
			-2.0f, 0.0f, 0.0f,	-2.0f, 0.0f, 0.0f,	-2.0f, 0.0f, 0.0f,
				//FrontLeft
			-2.0f, 0.0f, -2.0f,	-2.0f, 0.0f, -2.0f,	-2.0f, 0.0f, -2.0f,
			-2.0f, 0.0f, -2.0f,	-2.0f, 0.0f, -2.0f,	-2.0f, 0.0f, -2.0f,
	  
			//inside rectangles
			0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,	0.0f, 0.0f, 0.0f,
	  
			//top octagon ring
				//Back
			0.0f, 2.0f, 0.875f,		0.0f, 2.0f, 0.875f,			0.0f, 2.0f, 0.875f,
			0.0f, 2.0f, 0.875f,		0.0f, 2.0f, 0.875f,			0.0f, 2.0f, 0.875f,	  
				//Back Right
			0.625f, 2.0f, 0.625f,	0.625f, 2.0f, 0.625f,		0.625f, 2.0f, 0.625f,
			0.625f, 2.0f, 0.625f,	0.625f, 2.0f, 0.625f,		0.625f, 2.0f, 0.625f,
				//Right
			0.875f, 2.0f, 0.0f,		0.875f, 2.0f, 0.0f,			0.875f, 2.0f, 0.0f,
			0.875f, 2.0f, 0.0f,		0.875f, 2.0f, 0.0f,			0.875f, 2.0f, 0.0f,
				//Front Right
			0.625f, 2.0f, -0.625f,	0.625f, 2.0f, -0.625f,		0.625f, 2.0f, -0.625f,
			0.625f, 2.0f, -0.625f,	0.625f, 2.0f, -0.625f,		0.625f, 2.0f, -0.625f,	
				//Front
			0.0f, 2.0f, -0.875f,	0.0f, 2.0f, -0.875f,		0.0f, 2.0f, -0.875f,
			0.0f, 2.0f, -0.875f,	0.0f, 2.0f, -0.875f,		0.0f, 2.0f, -0.875f,	
				//Front Left
			-0.625f, 2.0f, -0.625f,	-0.625f, 2.0f, -0.625f,		-0.625f, 2.0f, -0.625f,
			-0.625f, 2.0f, -0.625f,	-0.625f, 2.0f, -0.625f,		-0.625f, 2.0f, -0.625f,	
				//Left
			-0.875f, 2.0f, 0.0f,	-0.875f, 2.0f, 0.0f,		-0.875f, 2.0f, 0.0f,
			-0.875f, 2.0f, 0.0f,	-0.875f, 2.0f, 0.0f,		-0.875f, 2.0f, 0.0f,	
				//Back Left
			-0.625f, 2.0f, 0.625f,	-0.625f, 2.0f, 0.625f,		-0.625f, 2.0f, 0.625f,
			-0.625f, 2.0f, 0.625f,	-0.625f, 2.0f, 0.625f,		-0.625f, 2.0f, 0.625f,	
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
		} // end for
		
		
		// sphere obj file imported object (code from chapter 6 - program 3)
		numObjVertices = sphere.getNumVertices();
		Vector3f[] svertices = sphere.getVertices();
		Vector2f[] stexCoords = sphere.getTexCoords();
		Vector3f[] snormals = sphere.getNormals();
		float[] spvalues = new float[numObjVertices*3];
		float[] stvalues = new float[numObjVertices*2];
		float[] snvalues = new float[numObjVertices*3];
		for (int i=0; i<numObjVertices; i++)
		{	spvalues[i*3]   = (float) (svertices[i]).x();
			spvalues[i*3+1] = (float) (svertices[i]).y();
			spvalues[i*3+2] = (float) (svertices[i]).z();
			stvalues[i*2]   = (float) (stexCoords[i]).x();
			stvalues[i*2+1] = (float) (stexCoords[i]).y();
			snvalues[i*3]   = (float) (snormals[i]).x();
			snvalues[i*3+1] = (float) (snormals[i]).y();
			snvalues[i*3+2] = (float) (snormals[i]).z();
		} // end for
		
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
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer cubeNorBuf = Buffers.newDirectFloatBuffer(cubeNormals);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeNorBuf.limit()*4, cubeNorBuf, GL_STATIC_DRAW);
		
		// pyramid
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer pyrBuf = Buffers.newDirectFloatBuffer(pyramidPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, pyrBuf.limit()*4, pyrBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer pyrTextBuf = Buffers.newDirectFloatBuffer(pyrTextureCoordinates);
		gl.glBufferData(GL_ARRAY_BUFFER, pyrTextBuf.limit()*4, pyrTextBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		FloatBuffer pyrNorBuf = Buffers.newDirectFloatBuffer(pyrNormals);
		gl.glBufferData(GL_ARRAY_BUFFER, pyrNorBuf.limit()*4, pyrNorBuf, GL_STATIC_DRAW);
		
		// octagon manual object
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		FloatBuffer octBuf = Buffers.newDirectFloatBuffer(octPos);
		gl.glBufferData(GL_ARRAY_BUFFER, octBuf.limit()*4, octBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		FloatBuffer octTexBuf = Buffers.newDirectFloatBuffer(octTexcoords);
		gl.glBufferData(GL_ARRAY_BUFFER, octTexBuf.limit()*4, octTexBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		FloatBuffer octNorBuf = Buffers.newDirectFloatBuffer(octNormals);
		gl.glBufferData(GL_ARRAY_BUFFER, octNorBuf.limit()*4, octNorBuf, GL_STATIC_DRAW);
		
		// complex cube imported obj file object
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		FloatBuffer complexCubeBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, complexCubeBuf.limit()*4, complexCubeBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		FloatBuffer complexCubeTexBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, complexCubeTexBuf.limit()*4, complexCubeTexBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		FloatBuffer complexCubeNorBuf = Buffers.newDirectFloatBuffer(nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, complexCubeNorBuf.limit()*4, complexCubeNorBuf, GL_STATIC_DRAW);
		
		// xyz axes
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[12]);
		FloatBuffer xyzBuf = Buffers.newDirectFloatBuffer(xyzAxesPos);
		gl.glBufferData(GL_ARRAY_BUFFER, xyzBuf.limit()*4, xyzBuf, GL_STATIC_DRAW);
		
		// skybox
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[13]);
		FloatBuffer cvertBuf = Buffers.newDirectFloatBuffer(cubePositions);
		gl.glBufferData(GL_ARRAY_BUFFER, cvertBuf.limit()*4, cvertBuf, GL_STATIC_DRAW);
		
		// sphere
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[14]);
		FloatBuffer sphereBuf = Buffers.newDirectFloatBuffer(spvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, sphereBuf.limit()*4, sphereBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[15]);
		FloatBuffer sphereTexBuf = Buffers.newDirectFloatBuffer(stvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, sphereTexBuf.limit()*4, sphereTexBuf, GL_STATIC_DRAW);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[16]);
		FloatBuffer sphereNorBuf = Buffers.newDirectFloatBuffer(snvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, sphereNorBuf.limit()*4, sphereNorBuf, GL_STATIC_DRAW);
	} // end setupVertices

	private void installLights()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		
		lightPos[0]=currentLightPos.x(); lightPos[1]=currentLightPos.y(); lightPos[2]=currentLightPos.z();
		
		// get the locations of the light and material fields in the shader
		globalAmbLoc = gl.glGetUniformLocation(renderingProgram, "globalAmbient");
		ambLoc = gl.glGetUniformLocation(renderingProgram, "light.ambient");
		diffLoc = gl.glGetUniformLocation(renderingProgram, "light.diffuse");
		specLoc = gl.glGetUniformLocation(renderingProgram, "light.specular");
		posLoc = gl.glGetUniformLocation(renderingProgram, "light.position");
		mambLoc = gl.glGetUniformLocation(renderingProgram, "material.ambient");
		mdiffLoc = gl.glGetUniformLocation(renderingProgram, "material.diffuse");
		mspecLoc = gl.glGetUniformLocation(renderingProgram, "material.specular");
		mshiLoc = gl.glGetUniformLocation(renderingProgram, "material.shininess");
		toggleLightLoc = gl.glGetUniformLocation(renderingProgram, "toggleLight");
	
		//  set the uniform light and material values in the shader
		gl.glProgramUniform4fv(renderingProgram, globalAmbLoc, 1, globalAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, ambLoc, 1, lightAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, lightDiffuse, 0);
		gl.glProgramUniform4fv(renderingProgram, specLoc, 1, lightSpecular, 0);
		gl.glProgramUniform3fv(renderingProgram, posLoc, 1, lightPos, 0);
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, matShi);
		gl.glProgramUniform1i(renderingProgram, toggleLightLoc, toggleLight % 2);
	} // end installLights

	public static void main(String[] args) { 
		new Code(); 
	} // end main
	
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		setupShadowBuffer();
	} // end reshape
	
	public void dispose(GLAutoDrawable drawable) {} // end dispose
} // end Code class
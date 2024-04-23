package a3;

import org.joml.*;

public class Camera	{
	private float camX, camY, camZ;
	private Vector3f camU, camV, camN;
	private Vector3f camLocation;
	private Matrix4f viewMat, viewRotate, viewTranslate;

	public Camera() {
		camX = 0.0f;
		camY = 0.0f;
		camZ = 8.0f;
		camLocation = new Vector3f(camX, camY, camZ);
		
		camU = new Vector3f(1.0f, 0.0f, 0.0f);
		camV = new Vector3f(0.0f, 1.0f, 0.0f);
		camN = new Vector3f(0.0f, 0.0f, -1.0f);
		
		viewMat = new Matrix4f();
		viewRotate = new Matrix4f();
		viewTranslate = new Matrix4f();
	} // end Camera Constructor
	
	// set camera location to a new location
	public void setCamLocation(Vector3f newLoc) { 
		camLocation.set(newLoc); 
	} // end setCamLocation
	
	// move camera forward based on a scalar
	public void moveCamForward(float scalar) {
		camN.mul(scalar);
		camLocation.add(camN);
	} // end moveCamForward
	
	// move camera backward based on a scalar
	public void moveCamBackward(float scalar) {
		camN.mul(scalar);
		camLocation.add(new Vector3f(-camN.x(), -camN.y(), -camN.z()));
	} // end moveCamBackward
		
	// move camera right based on a scalar
	public void moveCamRight(float scalar) {
		camU.mul(scalar);
		camLocation.add(camU);
	} // end moveCamRight
	
	// move camera left based on a scalar
	public void moveCamLeft(float scalar) {
		camU.mul(scalar);
		camLocation.add(new Vector3f(-camU.x(), -camU.y(), -camU.z()));
	} // end moveCamLeft
	
	// move camera up based on a scalar
	public void moveCamUp(float scalar) {
		camV.mul(scalar);
		camLocation.add(camV);
	} // end moveCamUp
	
	// move camera down based on a scalar
	public void moveCamDown(float scalar) {
		camV.mul(scalar);
		camLocation.add(new Vector3f(-camV.x(), -camV.y(), -camV.z()));
	} // end moveCamDown
	
	// turn (yaw) camera right based on an angle (amt)
	public void yawCamRight(float amt) {
		camU.rotateAxis(-amt, camV.x(), camV.y(), camV.z());
		camN.rotateAxis(-amt, camV.x(), camV.y(), camV.z());
	} // end yawCamRight
		
	// turn (yaw) camera left based on an angle (amt)
	public void yawCamLeft(float amt) {
		camU.rotateAxis(amt, camV.x(), camV.y(), camV.z());
		camN.rotateAxis(amt, camV.x(), camV.y(), camV.z());
	} // end yawCamLeft
	
	// turn (pitch) camera up based on an angle (amt)
	public void pitchCamUp(float amt) {
		camV.rotateAxis(amt, camU.x(), camU.y(), camU.z());
		camN.rotateAxis(amt, camU.x(), camU.y(), camU.z());
	} // end pitchCamUp
	
	// turn (pitch) camera down based on an angle (amt)
	public void pitchCamDown(float amt) {
		camV.rotateAxis(-amt, camU.x(), camU.y(), camU.z());
		camN.rotateAxis(-amt, camU.x(), camU.y(), camU.z());
	} // end pitchCamDown
	
	// roll camera left based on an angle (amt)
	public void rollCamLeft(float amt) {
		camU.rotateAxis(-amt, camN.x(), camN.y(), camN.z());
		camV.rotateAxis(-amt, camN.x(), camN.y(), camN.z());
	} // end rollCamLeft
	
	// roll camera right based on an angle (amt)
	public void rollCamRight(float amt) {
		camU.rotateAxis(amt, camN.x(), camN.y(), camN.z());
		camV.rotateAxis(amt, camN.x(), camN.y(), camN.z());
	} // end rollCamRight

	// create a view matrix
	public Matrix4f getViewMatrix() {
		viewTranslate.identity();
		viewTranslate.m30(-camLocation.x());
		viewTranslate.m31(-camLocation.y());
		viewTranslate.m32(-camLocation.z());

		viewRotate.set(camU.x(), camV.x(), -camN.x(), 0.0f,
					   camU.y(), camV.y(), -camN.y(), 0.0f,
					   camU.z(), camV.z(), -camN.z(), 0.0f,
					   0.0f, 	 0.0f, 	   0.0f, 	  1.0f);

		viewMat.identity();
		viewMat.mul(viewRotate);
		viewMat.mul(viewTranslate);

		return(viewMat);
	} // end getViewMatrix
} // end Camera Class
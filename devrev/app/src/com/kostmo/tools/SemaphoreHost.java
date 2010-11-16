package com.kostmo.tools;


public interface SemaphoreHost {

	void incSemaphore();
	void decSemaphore();
	
	void showError(String error);
 }
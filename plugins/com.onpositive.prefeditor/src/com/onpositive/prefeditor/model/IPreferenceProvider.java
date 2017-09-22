package com.onpositive.prefeditor.model;


public interface IPreferenceProvider {

	public abstract KeyValue[] getPrefsFor(String categoryName);

	public abstract void updateValue(KeyValue keyValue);

	public abstract void remove(KeyValue keyValue);

	public abstract void add(KeyValue newElement);

	public abstract void removeCategory(String category);

	public abstract String[] getNodeNames();

	public abstract void setUpdateCallback(IPreferenceUpdateCallback updateCallback);

	public abstract IPreferenceUpdateCallback getUpdateCallback();

	public abstract void setTracking(boolean tracking);

	public abstract boolean isTracking();

}
//package com.yourcompany.yoursetting;
package net.dinglisch.android.tasker;

import java.net.URISyntaxException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
 
// Constants and functions for Tasker *extensions* to the plugin protocol
// See Also: http://tasker.dinglisch.net/plugins.html
// v1.0b5

public class TaskerPlugin {

	private final static String 	TAG = "TaskerPlugin"; 

	private final static String 	BASE_KEY = "net.dinglisch.android.tasker";
	private final static String 	EXTRAS_PREFIX = BASE_KEY + ".extras.";
	
	/**
	 *	@see #addVariableBundle(Bundle, Bundle)  
	 *	@see Host#getVariablesBundle(Intent)
	 */
	private final static String		EXTRA_VARIABLES_BUNDLE = EXTRAS_PREFIX + "VARIABLES";

	/**
     * 	Host capabilities, passed to plugin with condition or setting edit intents 
     */
	private final static String		EXTRA_HOST_CAPABILITIES = EXTRAS_PREFIX + "HOST_CAPABILITIES";

	/**
     *  @see Setting#hostSupportsVariableReturn(Bundle)
     */
	public final static int			EXTRA_HOST_CAPABILITY_SETTING_RETURN_VARIABLES = 2;
	
	/**
     *	@see Condition#hostSupportsVariableReturn(Bundle)
     */
	public final static int			EXTRA_HOST_CAPABILITY_CONDITION_RETURN_VARIABLES = 4;

	/**
     * 	@see Setting#hostSupportsOnFireVariableReplacement(Bundle)
     */
	public final static int			EXTRA_HOST_CAPABILITY_SETTING_FIRE_VARIABLE_REPLACEMENT = 8;

	/**
     * @see Setting#hostSupportsVariableReturn(Bundle)
     */
	private final static int		EXTRA_HOST_CAPABILITY_RELEVANT_VARIABLES = 16;

	/**
     * 	
     */
	public final static int			EXTRA_HOST_CAPABILITY_SETTING_SYNCHRONOUS_EXECUTION = 32;


	public final static int			EXTRA_HOST_CAPABILITY_ALL = 
			EXTRA_HOST_CAPABILITY_SETTING_RETURN_VARIABLES | 
			EXTRA_HOST_CAPABILITY_CONDITION_RETURN_VARIABLES |
			EXTRA_HOST_CAPABILITY_SETTING_FIRE_VARIABLE_REPLACEMENT |
			EXTRA_HOST_CAPABILITY_RELEVANT_VARIABLES|
			EXTRA_HOST_CAPABILITY_SETTING_SYNCHRONOUS_EXECUTION;
	;

	// 
	/**
	 * 
     *	@see #hostSupportsRelevantVariables(Bundle)
     *  @see #addRelevantVariableList(Bundle, String[])
     *  @see #getRelevantVariableList(Bundle)
     */
	private final static String	BUNDLE_KEY_RELEVANT_VARIABLES = BASE_KEY + ".RELEVANT_VARIABLES";

	public static boolean hostSupportsRelevantVariables( Bundle extrasFromHost ) {
		return hostSupports( extrasFromHost,  EXTRA_HOST_CAPABILITY_RELEVANT_VARIABLES );
	}

	/**
 	* Specifies to host which variables might be used by the plugin.
 	* 
 	* Used in EditActivity, before setResult().
 	*
 	* @param  intentToHost the intent being returned to the host
 	* @param  variableNames array of relevant variable names
 	*/
	public static void addRelevantVariableList( Intent intentToHost, String [] variableNames ) {
		intentToHost.putExtra( BUNDLE_KEY_RELEVANT_VARIABLES, variableNames );
	}

	/**
	 * Allows the plugin/host to indicate to each other a set of variables which they are referencing.
	 * The host may use this to e.g. show a variable selection list in it's UI.
	 * The host should use this if it previously indicated to the plugin that it supports relevant vars
	 *
	 * @param  fromHostIntentExtras usually from getIntent().getExtras() 
	 * @return variableNames an array of relevant variable names
	*/
	public static String [] getRelevantVariableList( Bundle fromHostIntentExtras ) {

		String [] relevantVars = (String []) getBundleValueSafe( fromHostIntentExtras, BUNDLE_KEY_RELEVANT_VARIABLES, String [].class, "getRelevantVariableList" );
		
		if ( relevantVars == null )
			relevantVars = new String [0];
		
		return relevantVars;
	}

	/**
	 * Used by: plugin QueryReceiver, FireReceiver
	 *
	 * Add a bundle of variable name/value pairs.
	 *
	 * @param resultExtras the result extras from the receiver onReceive (from a call to getResultExtras())
	 * @param variables the variables to send
	 * @see #hostSupportsVariableReturn(Bundle)
	*/
	public static void addVariableBundle( Bundle resultExtras, Bundle variables ) {
		resultExtras.putBundle( EXTRA_VARIABLES_BUNDLE, variables );
	}

	// ----------------------------- SETTING PLUGIN ONLY --------------------------------- //

	public static class Setting {

		/**
		 *	@see #setVariableReplaceKeys(Bundle, String[])
	     */
		private final static String	BUNDLE_KEY_VARIABLE_REPLACE_STRINGS = EXTRAS_PREFIX + "VARIABLE_REPLACE_KEYS";

		/**
		 *	@see #requestTimeoutMS(Intent, int)
	     */
        private final static String EXTRA_REQUESTED_TIMEOUT = EXTRAS_PREFIX + "REQUESTED_TIMEOUT";
        
		 /**
         *      @see #signalFinish(Context, Intent, Status, Bundle)
         *  @see Host#addCompletionIntent(Intent, Intent)
         */
        private final static String EXTRA_PLUGIN_COMPLETION_INTENT = EXTRAS_PREFIX + "COMPLETION_INTENT";

		/**
         *  @see #signalFinish(Context, Intent, Status, Bundle)
         *  @see Host#getSettingCompletionStatus(Intent)
         */
        public final static String EXTRA_RESULT_CODE = EXTRAS_PREFIX + "RESULT_CODE";

		/**
		*  @see #signalFinish(Context, Intent, Status, Bundle)
        *  @see Host#getSettingResultCode(Intent)
        */

        public final static int	RESULT_CODE_OK = Activity.RESULT_OK;
        public final static int	RESULT_CODE_OK_MINOR_FAILURES = Activity.RESULT_FIRST_USER;
        public final static int	RESULT_CODE_FAILED = Activity.RESULT_FIRST_USER + 1;
        public final static int	RESULT_CODE_PENDING = Activity.RESULT_FIRST_USER + 2;
        public final static int	RESULT_CODE_UNKNOWN = Activity.RESULT_FIRST_USER + 3;
		
		/**
		 * Used by: plugin EditActivity.
		 * 
		 * Indicates to plugin that host will replace variables in specified bundle keys.
		 * 
		 * Replacement takes place every time the setting is fired, before the bundle is
		 * passed to the plugin FireReceiver.
		 *
		 * @param  extrasFromHost intent extras from the intent received by the edit activity
		 * @see #setVariableReplaceKeys(Bundle, String[])
		*/
		public static boolean hostSupportsOnFireVariableReplacement( Bundle extrasFromHost ) {
			return hostSupports( extrasFromHost, EXTRA_HOST_CAPABILITY_SETTING_FIRE_VARIABLE_REPLACEMENT );
		}
		
		public static boolean hostSupportsSynchronousExecution( Bundle extrasFromHost ) {
			return hostSupports( extrasFromHost, EXTRA_HOST_CAPABILITY_SETTING_SYNCHRONOUS_EXECUTION );
		}

		/**
	 	* Request the host to wait the specified number of milliseconds before continuing.
	 	* Note that the host may choose to ignore the request.
	 	* 
	 	* Used in EditActivity, before setResult().
	 	*
	 	* @param  intentToHost the intent being returned to the host
	 	* @param  timeoutMS 
	 	*/
		public static void requestTimeoutMS( Intent intentToHost, int timeoutMS ) {
			intentToHost.putExtra( EXTRA_REQUESTED_TIMEOUT, timeoutMS );
		}

		/**
		 * Used by: plugin EditActivity 
		 *
		 * Indicates to host which bundle keys should be replaced.
		 *
		 * @param  resultBundleToHost the bundle being returned to the host
		 * @param  listOfKeyNames which bundle keys to replace variables in when setting fires
		 * @see #hostSupportsOnFireVariableReplacement(Bundle)
		*/
		public static void setVariableReplaceKeys( Bundle resultBundleToHost, String [] listOfKeyNames ) {
			
			StringBuilder builder = new StringBuilder();
			
			if ( listOfKeyNames != null ) {
				
				for ( String keyName : listOfKeyNames ) {
				
					if ( keyName.contains( " " ) )
						Log.w( TAG, "setVariableReplaceKeys: ignoring bad keyName containing space: " + keyName );
					else {
						if ( builder.length() > 0 )
							builder.append( ' ' );
						
						builder.append( keyName );
					}
					
					if ( builder.length() > 0 )
						resultBundleToHost.putString( BUNDLE_KEY_VARIABLE_REPLACE_STRINGS, builder.toString() );
				}
			}
		}

		/**
		 * Used by: plugin FireReceiver 
		 *
		 * Indicates to plugin whether the host will process variables which it passes back
		 *
		 * @param  extrasFromHost intent extras from the intent received by the FireReceiver
		 * @see #signalFinish(Context, Intent, Status, Bundle)
		*/
		public static boolean hostSupportsVariableReturn( Bundle extrasFromHost ) {
			return hostSupports( extrasFromHost,  EXTRA_HOST_CAPABILITY_SETTING_RETURN_VARIABLES );
		}
		
		 /**
         * Used by: plugin FireReceiver 
         *
         * Tell the host that the plugin has finished execution.
         *
         * @param originalFireIntent the intent received from the host (via onReceive())
         * @param status level of success in performing the settings
         * @param vars any variables that the plugin wants to set in the host
         * @see #hostSupportsSynchronousSettings(Bundle)
         * @see #setWantSynchronousExecution(Intent, int)
        */
        public static boolean signalFinish( Context context, Intent originalFireIntent, int resultCode, Bundle vars ) {
        
        	String errorPrefix = "signalFinish: ";
        
        	boolean okFlag = false;

        	String completionIntentString = (String) TaskerPlugin.getExtraValueSafe( originalFireIntent, Setting.EXTRA_PLUGIN_COMPLETION_INTENT, String.class, "signalFinish" );

        	if ( completionIntentString != null ) {
        		Uri completionIntentUri = null;
        		try {
        			completionIntentUri = Uri.parse( completionIntentString );
        		}
        		// 	should only throw NullPointer but don't particularly trust it
        		catch ( Exception e ) {
        			Log.w( TAG, errorPrefix + "couldn't parse " + completionIntentString );
        		}
        	
        		if ( completionIntentUri != null ) {
        			try {
        				Intent completionIntent = Intent.parseUri( completionIntentString, Intent.URI_INTENT_SCHEME );
            
        				completionIntent.putExtra( EXTRA_RESULT_CODE, resultCode );
                                                        
        				if ( vars != null )
        					completionIntent.putExtra( EXTRA_VARIABLES_BUNDLE, vars );
                                                        
        				context.sendBroadcast( completionIntent );
        			
        				okFlag = true;
        			}
        			catch ( URISyntaxException e ) {
        				Log.w( TAG, errorPrefix + "bad URI: " + completionIntentUri );
        			}
        		}
        	}
                
        	return okFlag;
        }
	}
		
	// ----------------------------- CONDITION PLUGIN ONLY --------------------------------- //
	
	public static class Condition {

		/**
		 * Used by: plugin QueryReceiver 
		 *
		 * Indicates to plugin whether the host will process variables which it passes back
		 *
		 * @param  extrasFromHost intent extras from the intent received by the QueryReceiver
		 * @see #addVariableBundle(Bundle, Bundle)
		*/
		public static boolean hostSupportsVariableReturn( Bundle extrasFromHost ) {
			return hostSupports( extrasFromHost,  EXTRA_HOST_CAPABILITY_CONDITION_RETURN_VARIABLES );
		}
	}

	// ---------------------------------- HOST  ----------------------------------------- //

	public static class Host {

		/**
		 * Tell the plugin what capabilities the host support. This should be called when sending
		 * intents to any EditActivity, FireReceiver or QueryReceiver.
		 *
		 * @param  toPlugin the intent we're sending
		 * @return capabilites one or more of the EXTRA_HOST_CAPABILITY_XXX flags 
		*/
		public static Intent addCapabilities( Intent toPlugin, int capabilities ) {
			return toPlugin.putExtra( EXTRA_HOST_CAPABILITIES, capabilities  );
		}

		 /**
         * Add an intent to the fire intent before it goes to the plugin FireReceiver, which the plugin
         * can use to signal when it is finished. Only use if @code{pluginWantsSychronousExecution} is true.
         *
         * @param fireIntent fire intent going to the plugin 
         * @param completionIntent intent which will signal the host that the plugin is finished.
         * Implementation is host-dependent.
        */
        public static void addCompletionIntent( Intent fireIntent, Intent completionIntent ) {
                fireIntent.putExtra( 
                        Setting.EXTRA_PLUGIN_COMPLETION_INTENT, 
                        completionIntent.toUri( Intent.URI_INTENT_SCHEME )
                );
        }

        /**
         * When a setting plugin is finished, it sends the host the intent which was passed to it
         * via @code{addCompletionIntent}. 
         *
         * @param completionIntent intent returned from the plugin when it finished.
         * @return completionStatus measure of plugin success, defaults to UNKNOWN 
        */
        public static int getSettingResultCode( Intent completionIntent ) {
        
        	Integer val = (Integer) getExtraValueSafe( completionIntent, Setting.EXTRA_RESULT_CODE, String.class, "getSettingCompletionStatus" );

        	return ( val == null ) ? Setting.RESULT_CODE_UNKNOWN : val;
        }

		/**
		 * Extract a bundle of variables from an intent received from the FireReceiver. This
		 * should be called if the host previously indicated to the plugin
		 * that it supports setting variable return.
		 *
		 * @param  fromPlugin the intent we received
		 * @return variables a bundle of variable name/value pairs
		 * @see #addCapabilities(Intent, int) 
		*/

		public static Bundle getVariablesBundle( Bundle resultExtras ) {
			return (Bundle) getBundleValueSafe( 
					resultExtras, EXTRA_VARIABLES_BUNDLE, Bundle.class, "getVariablesBundle" 
			);
		}

		public static boolean haveRequestedTimeout( Bundle extrasFromPluginEditActivity ) {
			return extrasFromPluginEditActivity.containsKey( Setting.EXTRA_REQUESTED_TIMEOUT );
		}
		
		public static int getRequestedTimeoutMS( Bundle extrasFromPluginEditActivity ) {
			return 
					(Integer) getBundleValueSafe( 
							extrasFromPluginEditActivity, Setting.EXTRA_REQUESTED_TIMEOUT,	Integer.class, "getRequestedTimeout" 
					)
			;
		}
		
		public static String [] getSettingVariableReplaceKeys( Bundle fromPluginEditActivity ) {

			String spec = (String) TaskerPlugin.getBundleValueSafe( fromPluginEditActivity, Setting.BUNDLE_KEY_VARIABLE_REPLACE_STRINGS, String.class, "getSettingVariableReplaceKeys" );

			String [] replaceKeys = null;
			
			if ( spec != null )
				replaceKeys = spec.split( " " );
			
			return replaceKeys;
		}
		
		public static boolean haveRelevantVariables( Bundle b ) {
			return b.containsKey( BUNDLE_KEY_RELEVANT_VARIABLES );
		}
		
		public static void cleanRelevantVariables( Bundle b ) {
			b.remove( BUNDLE_KEY_RELEVANT_VARIABLES );
		}

		public static void cleanRequestedTimeout( Bundle extras ) {
			extras.remove( Setting.EXTRA_REQUESTED_TIMEOUT );
		}
		
		public static void cleanSettingReplaceVariables( Bundle b ) {
			b.remove( Setting.BUNDLE_KEY_VARIABLE_REPLACE_STRINGS );
		}
	}
	
	// ---------------------------------- HELPER FUNCTIONS -------------------------------- //

	private static Object getBundleValueSafe( Bundle b, String key, Class<?> expectedClass, String funcName ) {
		Object value = null;
		
		if ( b != null ) {
			if ( b.containsKey( key ) ) {
				Object obj = b.get( key );
				if ( obj == null )
					Log.w( TAG, funcName + ": " + key + ": null value" );
				else if ( obj.getClass() != expectedClass ) 
					Log.w( TAG, funcName + ": " + key + ": expected " + expectedClass.getClass().getName() + ", got " + obj.getClass().getName() );
				else
					value = obj;
			}
		}
		return value;
	}
	
	private static Object getExtraValueSafe( Intent i, String key, Class<?> expectedClass, String funcName ) {
		return ( i.hasExtra( key ) ) ?
                 getBundleValueSafe( i.getExtras(), key, expectedClass, funcName ) :
                 null;
	}

	private static boolean hostSupports( Bundle extrasFromHost, int capabilityFlag ) {
		Integer flags = (Integer) getBundleValueSafe( extrasFromHost, EXTRA_HOST_CAPABILITIES, Integer.class, "hostSupports" );
		return 
				( flags != null ) &&
				( ( flags & capabilityFlag ) > 0 )
			;
	}
	
}
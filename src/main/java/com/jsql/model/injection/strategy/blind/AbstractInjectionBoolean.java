package com.jsql.model.injection.strategy.blind;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.jsql.model.InjectionModel;
import com.jsql.model.bean.util.Interaction;
import com.jsql.model.bean.util.Request;
import com.jsql.model.exception.InjectionFailureException;
import com.jsql.model.exception.StoppedByUserSlidingException;
import com.jsql.model.suspendable.AbstractSuspendable;
import com.jsql.model.suspendable.callable.ThreadFactoryCallable;

public abstract class AbstractInjectionBoolean<T extends AbstractCallableBoolean<T>> {
    
    /**
     * Every FALSE SQL statements will be checked,
     * more statements means a more robust application
     */
    protected List<String> falseTest;
    
    /**
     * Every TRUE SQL statements will be checked,
     * more statements means a more robust application
     */
    protected List<String> trueTest;
    
    /**
     * Constant linked to a URL, true if that url
     * checks the end of the SQL result, false otherwise.
     */
    protected static final boolean IS_TESTING_LENGTH = true;
    
    /**
     * Log4j logger sent to view.
     */
    private static final Logger LOGGER = Logger.getRootLogger();
    
    public enum BooleanMode {
        AND, OR
    }
    
    protected InjectionModel injectionModel;
    
    protected BooleanMode booleanMode;
    
    public AbstractInjectionBoolean(InjectionModel injectionModel, BooleanMode booleanMode) {
        
        this.injectionModel = injectionModel;
        this.booleanMode = booleanMode;

        this.falseTest = this.injectionModel.getMediatorVendor().getVendor().instance().getListFalseTest();
        this.trueTest = this.injectionModel.getMediatorVendor().getVendor().instance().getListTrueTest();
    }
    
    public abstract T getCallable(String string, int indexCharacter, boolean isTestingLength);
    
    public abstract T getCallable(String string, int indexCharacter, int bit);
    
    /**
     * Start one test to verify if boolean works.
     * @return true if boolean method is confirmed
     * @throws InjectionFailureException
     */
    public abstract boolean isInjectable() throws StoppedByUserSlidingException;
    
    /**
     * Display a message to explain how is blid/time working.
     * @return
     */
    public abstract String getInfoMessage();

    /**
     * Process the whole boolean injection, character by character, bit by bit.
     * @param inj SQL query
     * @param suspendable Action a user can stop
     * @return Final string: SQLiABCDEF...
     * @throws StoppedByUserSlidingException
     */
    public String inject(String inj, AbstractSuspendable<String> suspendable) throws StoppedByUserSlidingException {

        // List of the characters, each one represented by an array of 8 bits
        // e.g SQLi: bytes[0] => 01010011:S, bytes[1] => 01010001:Q ...
        List<char[]> bytes = new ArrayList<>();
        
        // Cursor for current character position
        int indexCharacter = 0;

        // Concurrent URL requests
        ExecutorService taskExecutor = Executors.newCachedThreadPool(new ThreadFactoryCallable("CallableAbstractBoolean"));
        CompletionService<T> taskCompletionService = new ExecutorCompletionService<>(taskExecutor);

        // Send the first binary question: is the SQL result empty?
        taskCompletionService.submit(this.getCallable(inj, 0, IS_TESTING_LENGTH));
        
        // Increment the number of active tasks
        int countTasksSubmitted = 1;
        int countBadAsciiCode = 0;

        
        // Process the job until there is no more active task,
        // in other word until all HTTP requests are done
        while (countTasksSubmitted > 0) {
            
            // TODO Coverage with pausable multithreading
            if (suspendable.isSuspended()) {
                
                String result = this.stop(bytes, taskExecutor);
                throw new StoppedByUserSlidingException(result);
            }
            
            try {
                // The URL call is done, bring back the finished task
                T currentCallable = taskCompletionService.take().get();
                
                // One task has just ended, decrease active tasks by 1
                countTasksSubmitted--;
                
                // If SQL result is not empty, then add a new unknown character,
                // and define a new array of 8 undefined bit.
                // Then add a new length verification, and all 8 bits
                // requests for that new character.
                if (currentCallable.isTestingLength()) {
                    
                    if (currentCallable.isTrue()) {
                        
                        indexCharacter++;
                        
                        // New undefined bits of the next character
                        bytes.add(new char[]{'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x'});
                        
                        // Test if it's the end of the line
                        taskCompletionService.submit(this.getCallable(inj, indexCharacter, IS_TESTING_LENGTH));
                        
                        // Test the 8 bits for the next character, save its position and current bit for later
                        for (int bit: new int[]{1, 2, 4, 8, 16, 32, 64, 128}) {
                            
                            taskCompletionService.submit(this.getCallable(inj, indexCharacter, bit));
                        }
                        
                        // Add 9 new tasks
                        countTasksSubmitted += 9;
                    }
                    
                } else {
                    
                    // Process url that has just checked one bit, convert bits to chars,
                    // and change current bit from undefined to 0 or 1
                    
                    char[] asciiCodeMask = this.initializeBinaryMask(bytes, currentCallable);
                    
                    String asciiCodeBinary = new String(asciiCodeMask);
                    
                    // Inform the View if bits array is complete, else nothing #Need fix
                    if (asciiCodeBinary.matches("^[01]{8}$")) {
                        
                        int asciiCode = Integer.parseInt(asciiCodeBinary, 2);
                        
                        if (asciiCode == 255 || asciiCode == 0) {
                            
                            if (
                                countTasksSubmitted != 0
                                && countBadAsciiCode > 9
                                && (countBadAsciiCode * 100 / countTasksSubmitted) > 50
                            ) {
                                LOGGER.warn("Boolean false positives spotted, stopping...");
                                break;
                            }
                            
                            countBadAsciiCode++;
                        }

                        String charText = Character.toString((char) asciiCode);
                        
                        Request interaction = new Request();
                        interaction.setMessage(Interaction.MESSAGE_BINARY);
                        interaction.setParameters(asciiCodeBinary +"="+ charText.replaceAll("\\n", "\\\\\\n").replaceAll("\\r", "\\\\\\r").replaceAll("\\t", "\\\\\\t"));
                        this.injectionModel.sendToViews(interaction);
                    }
                }
                
            } catch (InterruptedException | ExecutionException e) {
                
                LOGGER.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }

        return this.stop(bytes, taskExecutor);
    }

    private char[] initializeBinaryMask(List<char[]> bytes, T currentCallable) {
        
        // Bits for current url
        char[] asciiCodeMask = bytes.get(currentCallable.getCurrentIndex() - 1);
        
        int positionInMask = (int) (8 - (Math.log(2) + Math.log(currentCallable.getCurrentBit())) / Math.log(2));
        
        // Set current bit
        asciiCodeMask[positionInMask] = currentCallable.isTrue() ? '1' : '0';
        
        return asciiCodeMask;
    }

    private String stop(List<char[]> bytes, ExecutorService taskExecutor) {
        
        // Await for termination
        boolean isTerminated = false;
        
        try {
            
            taskExecutor.shutdown();
            isTerminated = taskExecutor.awaitTermination(0, TimeUnit.SECONDS);
            
        } catch (InterruptedException e) {
            
            LOGGER.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
        
        if (!isTerminated) {
            
            // awaitTermination timed out, interrupt everything
            taskExecutor.shutdownNow();
        }

        // Get current progress and display
        StringBuilder result = new StringBuilder();
        
        for (char[] c: bytes) {
            
            try {
                
                int charCode = Integer.parseInt(new String(c), 2);
                String str = Character.toString((char) charCode);
                result.append(str);
                
            } catch (NumberFormatException err) {
                // Byte string not fully constructed : 0x1x010x
                // Ignore
            }
        }
        
        return result.toString();
    }

    /**
     * Run a HTTP call via the model.
     * @param urlString URL to inject
     * @return Source code
     */
    public String callUrl(String urlString) {
        return this.injectionModel.injectWithoutIndex(urlString);
    }
}

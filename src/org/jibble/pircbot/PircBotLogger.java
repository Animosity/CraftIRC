package org.jibble.pircbot;

public interface PircBotLogger {

    /**
     * Adds a line to the log.  This log is currently output to the standard
     * output and is in the correct format for use by tools such as pisg, the
     * Perl IRC Statistics Generator.  You may override this method if you wish
     * to do something else with log entries.
     * Each line in the log begins with a number which
     * represents the logging time (as the number of milliseconds since the
     * epoch).  This timestamp and the following log entry are separated by
     * a single space character, " ".  Outgoing messages are distinguishable
     * by a log entry that has ">>>" immediately following the space character
     * after the timestamp.  DCC events use "+++" and warnings about unhandled
     * Exceptions and Errors use "###".
     *  <p>
     * This implementation of the method will only cause log entries to be
     * output if the PircBot has had its verbose mode turned on by calling
     * setVerbose(true);
     * 
     * @param line The line to add to the log.
     */
    void log(String line);

}

package org.brylex.maven.command;

import org.sonatype.gshell.command.Command;
import org.sonatype.gshell.command.CommandContext;
import org.sonatype.gshell.command.registry.CommandRegistrar;
import org.sonatype.gshell.command.support.CommandActionSupport;
import org.sonatype.gshell.notification.ExitNotification;

import javax.inject.Inject;

@Command(name = "quit")
public class QuitCommand extends CommandActionSupport
{
    public Object execute(CommandContext commandContext) throws Exception
    {
        System.err.println("QUIT!!!!");
        throw new ExitNotification(0);
    }
}

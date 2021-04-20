package net.openhft.chronicle.releasenotes.cli.mixin;

import picocli.CommandLine.Command;

@Command(
    descriptionHeading   = "%nDescription: ",
    parameterListHeading = "%nParameters:%n%n",
    optionListHeading    = "%nOptions:%n",
    commandListHeading   = "%nCommands:%n",
    sortOptions = false
)
public final class CommandPresetMixin {
}

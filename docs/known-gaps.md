# Known gaps

What is missing or unseen, and what to do when it becomes possible. The same list is on the wiki page "Versions and known gaps".

## Not done

- Item counts above 99 are shown as 99 in the panel.
- Forge: JEI, REI and Jade have no 26.2 or 26.3 build, so the plus buttons (JEI, REI) and the Jade lines exist only on NeoForge. Add them when those mods release for Forge.
- EMI has no 26.2 or 26.3 build for either loader: no EMI plus button yet. Add it when EMI releases for 26.2 and 26.3.
- KubeJS has no 26.2 or 26.3 build. The KubeJS side is a startup script that hands scripts the same API object ModJS gets; it is covered by a Rhino unit test and has not run inside KubeJS. Run it in KubeJS when a 26.x build exists.
- The JEI and REI plus buttons are compiled against the real 26.2 and 26.3 NeoForge APIs but were not clicked in a client. The BEI plus button runs the real BEI transfer host and the real handler of the station (see the testing notes), not a click on BEI's popup.
- Show Me What This Is and Jade lines were written against their APIs and were not seen on a real client.
- Multiplayer, long sessions and modded storage blocks have not been tried beyond what the self test covers.

Set objFSO = CreateObject("Scripting.FileSystemObject")
Set objShell = CreateObject("WScript.Shell")

scriptDir = objFSO.GetParentFolderName(WScript.ScriptFullName)
jarPath = scriptDir & "\target\file-crypto-tool-1.0.0.jar"

' Detect javaw path
javawPath = "javaw.exe"
If objFSO.FileExists("C:\Program Files\Java\jdk-18.0.2.1\bin\javaw.exe") Then
    javawPath = "C:\Program Files\Java\jdk-18.0.2.1\bin\javaw.exe"
End If

cmd = """" & javawPath & """ -jar """ & jarPath & """"

' Run without showing any terminal window (0 = hidden)
objShell.CurrentDirectory = scriptDir
objShell.Run cmd, 0, False

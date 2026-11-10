Set objFSO = CreateObject("Scripting.FileSystemObject")
Set objShell = CreateObject("WScript.Shell")

scriptDir = objFSO.GetParentFolderName(WScript.ScriptFullName)
jarPath = scriptDir & "\target\file-crypto-tool-1.0.0.jar"

' Detect javaw path
javawPath = "javaw.exe"
If objFSO.FileExists("C:\Program Files\Java\jdk-18.0.2.1\bin\javaw.exe") Then
    javawPath = "C:\Program Files\Java\jdk-18.0.2.1\bin\javaw.exe"
ElseIf objShell.Environment("PROCESS").Item("JAVA_HOME") <> "" Then
    javaHome = objShell.Environment("PROCESS").Item("JAVA_HOME")
    If objFSO.FileExists(javaHome & "\bin\javaw.exe") Then
        javawPath = javaHome & "\bin\javaw.exe"
    End If
End If

' Check if JAR exists; if not, prompt user to build with run.bat
If Not objFSO.FileExists(jarPath) Then
    res = MsgBox("The application package was not found:" & vbCrLf & _
                 jarPath & vbCrLf & vbCrLf & _
                 "Would you like to run 'run.bat' to build and start the application?", _
                 vbYesNo + vbQuestion, "File Encryption & Decryption System")
    If res = vbYes Then
        objShell.CurrentDirectory = scriptDir
        objShell.Run "cmd.exe /c """ & scriptDir & "\run.bat""", 1, False
    End If
    WScript.Quit
End If

cmd = """" & javawPath & """ -jar """ & jarPath & """"

' Run without showing any terminal window (0 = hidden)
objShell.CurrentDirectory = scriptDir
objShell.Run cmd, 0, False

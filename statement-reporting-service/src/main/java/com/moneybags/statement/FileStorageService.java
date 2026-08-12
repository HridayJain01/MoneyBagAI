package com.moneybags.statement;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
class FileStorageService {
    record Stored(String key,long size,String checksum) {}
    private final Path root;
    FileStorageService(StatementProperties p){try{root=Path.of(p.getStorageDirectory()).toAbsolutePath().normalize();Files.createDirectories(root);}catch(IOException e){throw new IllegalStateException("Cannot initialize statement storage",e);}}
    Stored store(String key,byte[] bytes){
        Path target=resolve(key),tmp=null;try{Files.createDirectories(target.getParent());tmp=Files.createTempFile(target.getParent(),"statement-",".tmp");Files.write(tmp,bytes,StandardOpenOption.TRUNCATE_EXISTING);try{Files.move(tmp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);}return new Stored(key,bytes.length,sha(bytes));}
        catch(IOException e){if(tmp!=null)try{Files.deleteIfExists(tmp);}catch(IOException ignored){}throw new IllegalStateException("File storage failed",e);}
    }
    byte[] read(String key){try{return Files.readAllBytes(resolve(key));}catch(IOException e){throw ApiException.notFound("FILE_NOT_FOUND","Generated file is unavailable");}}
    private Path resolve(String key){Path p=root.resolve(key).normalize();if(!p.startsWith(root))throw ApiException.forbidden("INVALID_STORAGE_KEY","Invalid storage key");return p;}
    private String sha(byte[] b){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}catch(Exception e){throw new IllegalStateException(e);}}
}

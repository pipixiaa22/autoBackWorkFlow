package com.ckrey.autobackworkflow.export.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ckrey.autobackworkflow.common.exception.BizException;
import com.ckrey.autobackworkflow.common.api.CursorPage;
import com.ckrey.autobackworkflow.common.util.Hashing;
import com.ckrey.autobackworkflow.domain.AdsAudioAsset;
import com.ckrey.autobackworkflow.domain.AdsDialogueSegment;
import com.ckrey.autobackworkflow.domain.AdsExportRecord;
import com.ckrey.autobackworkflow.domain.AdsProject;
import com.ckrey.autobackworkflow.project.application.ProjectApplicationService;
import com.ckrey.autobackworkflow.service.AdsAudioAssetService;
import com.ckrey.autobackworkflow.service.AdsDialogueSegmentService;
import com.ckrey.autobackworkflow.service.AdsExportRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportApplicationService {
    private final ProjectApplicationService projects; private final AdsDialogueSegmentService segments; private final AdsAudioAssetService assets; private final AdsExportRecordService exports; private final SrtGenerator srt; private final ObjectMapper mapper; private final Path root;
    public ExportApplicationService(ProjectApplicationService projects, AdsDialogueSegmentService segments, AdsAudioAssetService assets, AdsExportRecordService exports, SrtGenerator srt, ObjectMapper mapper, @Value("${ads.storage.root:./data/assets}") String root) { this.projects=projects;this.segments=segments;this.assets=assets;this.exports=exports;this.srt=srt;this.mapper=mapper;this.root=Path.of(root).toAbsolutePath().normalize(); }
    @Transactional public AdsExportRecord exportSrt(Long projectId,String idempotencyKey) { return export(projectId,"SRT",idempotencyKey,false); }
    @Transactional public AdsExportRecord exportPackage(Long projectId,String idempotencyKey) { return export(projectId,"PACKAGE",idempotencyKey,true); }
    public AdsExportRecord required(Long id) { AdsExportRecord v=exports.getById(id);if(v==null)throw BizException.notFound("EXPORT_NOT_FOUND","导出记录不存在");return v; }
    public CursorPage<AdsExportRecord> list(Long projectId, String cursor, Integer requestedLimit) {
        projects.required(projectId);
        int limit = CursorPage.limit(requestedLimit);
        CursorPage.Cursor anchor = CursorPage.decode(cursor);
        var query = Wrappers.<AdsExportRecord>lambdaQuery().eq(AdsExportRecord::getProjectId, projectId);
        if (anchor != null) {
            query.and(group -> group.lt(AdsExportRecord::getCreatedAt, anchor.createdAt())
                    .or(tie -> tie.eq(AdsExportRecord::getCreatedAt, anchor.createdAt())
                            .lt(AdsExportRecord::getId, anchor.id())));
        }
        List<AdsExportRecord> rows = exports.list(query.orderByDesc(AdsExportRecord::getCreatedAt)
                .orderByDesc(AdsExportRecord::getId).last("LIMIT " + (limit + 1)));
        return CursorPage.from(rows, limit, AdsExportRecord::getCreatedAt, AdsExportRecord::getId);
    }
    public Resource download(Long id) { AdsExportRecord record=required(id); if(!"SUCCEEDED".equals(record.getStatus()))throw BizException.badRequest("EXPORT_NOT_READY","导出文件尚未就绪");Path path=resolve(record.getObjectKey());if(!Files.isRegularFile(path))throw BizException.notFound("EXPORT_FILE_MISSING","导出文件不存在或已过期");return new FileSystemResource(path); }
    private AdsExportRecord export(Long projectId,String type,String key,boolean packageExport) { AdsProject project=projects.required(projectId); if(key!=null&&!key.isBlank()){AdsExportRecord existing=exports.getOne(Wrappers.<AdsExportRecord>lambdaQuery().eq(AdsExportRecord::getProjectId,projectId).eq(AdsExportRecord::getExportType,type).eq(AdsExportRecord::getIdempotencyKey,key));if(existing!=null)return existing;}
        List<AdsDialogueSegment> dialogue=segments.list(Wrappers.<AdsDialogueSegment>lambdaQuery().eq(AdsDialogueSegment::getProjectId,projectId).eq(AdsDialogueSegment::getDeleted,0).orderByAsc(AdsDialogueSegment::getSegmentNo));if(dialogue.isEmpty())throw BizException.badRequest("EXPORT_NO_SEGMENTS","项目尚无可导出的台词分段");
        Date now=new Date();AdsExportRecord record=new AdsExportRecord();record.setExportNo("EX-"+UUID.randomUUID().toString().replace("-", "").substring(0,12).toUpperCase());record.setProjectId(projectId);record.setExportType(type);record.setStatus("RUNNING");record.setIdempotencyKey(key);record.setProjectVersion(project.getVersion());record.setSnapshotJson(Hashing.json(java.util.Map.of("projectId",projectId,"segments",dialogue)));record.setStorageProvider("LOCAL");record.setVersion(1);record.setCreatedAt(now);record.setUpdatedAt(now);record.setStartedAt(now);exports.save(record);
        try { String safe=safeName(project.getName());String suffix=packageExport?".zip":".srt";String objectKey="exports/"+record.getExportNo()+"/"+safe+suffix;Path target=resolve(objectKey);Files.createDirectories(target.getParent());Path temporary=Files.createTempFile(target.getParent(),"export-",".tmp");if(packageExport)writePackage(temporary,project,dialogue);else Files.writeString(temporary,srt.generate(dialogue),StandardCharsets.UTF_8);Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);record.setStatus("SUCCEEDED");record.setObjectKey(objectKey);record.setFileName(safe+suffix);record.setFileSizeBytes(Files.size(target));record.setSha256(hashFile(target));record.setFinishedAt(new Date());exports.updateById(record);}catch(IOException ex){record.setStatus("FAILED");record.setErrorCode("EXPORT_WRITE_FAILED");record.setErrorMessage("导出文件写入失败");record.setFinishedAt(new Date());exports.updateById(record);}return required(record.getId()); }
    private void writePackage(Path file,AdsProject project,List<AdsDialogueSegment> dialogue)throws IOException {try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(file),StandardCharsets.UTF_8)){put(zip,"source/story-background.txt",project.getStoryBackground()==null?"":project.getStoryBackground());put(zip,"source/dialogue-original.txt",project.getOriginalDialogue());put(zip,"subtitle/jianying_tts.srt",srt.generate(dialogue));put(zip,"metadata/dialogue.json",mapper.writeValueAsString(dialogue));put(zip,"metadata/generation-manifest.json",mapper.writeValueAsString(assets.list(Wrappers.<AdsAudioAsset>lambdaQuery().eq(AdsAudioAsset::getProjectId,project.getId()).eq(AdsAudioAsset::getDeleted,0))));put(zip,"README.txt","Audio Dialogue Studio export: "+safeName(project.getName()));}}
    private static void put(ZipOutputStream zip,String name,String content)throws IOException{zip.putNextEntry(new ZipEntry(name));zip.write(content.getBytes(StandardCharsets.UTF_8));zip.closeEntry();}
    private Path resolve(String key){Path path=root.resolve(key).normalize();if(!path.startsWith(root))throw BizException.badRequest("EXPORT_INVALID_PATH","非法导出路径");return path;}
    private static String safeName(String value){String n=value==null?"project":value.replaceAll("[^\\p{IsHan}A-Za-z0-9._-]","_");return n.isBlank()?"project":n.length()>80?n.substring(0,80):n;}
    private static String hashFile(Path path)throws IOException{try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));StringBuilder s=new StringBuilder();for(byte b:digest)s.append(String.format("%02x",b));return s.toString();}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}

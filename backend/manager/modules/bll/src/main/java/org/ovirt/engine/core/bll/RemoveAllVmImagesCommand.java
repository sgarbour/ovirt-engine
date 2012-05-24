package org.ovirt.engine.core.bll;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.common.action.RemoveAllVmImagesParameters;
import org.ovirt.engine.core.common.action.RemoveImageParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

/**
 * This command removes all Vm images and all created snapshots both from Irs
 * and Db.
 */
@InternalCommandAttribute
@NonTransactiveCommandAttribute
public class RemoveAllVmImagesCommand<T extends RemoveAllVmImagesParameters> extends VmCommand<T> {

    private static final long serialVersionUID = 3577196516027044528L;

    public RemoveAllVmImagesCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected void ExecuteVmCommand() {
        Set<Guid> mImagesToBeRemoved = new HashSet<Guid>();
        List<DiskImage> images = getParameters().Images;
        if (images == null) {
            images =
                    ImagesHandler.filterImageDisks(DbFacade.getInstance().getDiskDao().getAllForVm(getVmId()),
                            true,
                            false);
        }
        for (DiskImage image : images) {
            if (Boolean.TRUE.equals(image.getactive())) {
                mImagesToBeRemoved.add(image.getImageId());
            }
        }

        boolean noImagesRemovedYet = true;
        for (DiskImage image : images) {
            if (mImagesToBeRemoved.contains(image.getImageId())) {
                RemoveImageParameters tempVar = new RemoveImageParameters(image.getImageId());
                tempVar.setParentCommand(getParameters().getParentCommand());
                tempVar.setParentParemeters(getParameters().getParentParameters());
                tempVar.setDiskImage(image);
                tempVar.setEntityId(getParameters().getEntityId());
                tempVar.setForceDelete(getParameters().getForceDelete());
                tempVar.setParentParemeters(getParameters());
                VdcReturnValueBase vdcReturnValue =
                        Backend.getInstance().runInternalAction(VdcActionType.RemoveImage,
                                tempVar,
                                ExecutionHandler.createDefaultContexForTasks(getExecutionContext()));

                if (vdcReturnValue.getSucceeded()) {
                    getReturnValue().getInternalTaskIdList().addAll(vdcReturnValue.getInternalTaskIdList());
                } else {
                    if (noImagesRemovedYet) {
                        setSucceeded(false);
                        getReturnValue().setFault(vdcReturnValue.getFault());
                        return;
                    }

                    log.errorFormat("Can't remove image id: {0} for VM id: {1} due to: {2}.",
                            image.getImageId(), getParameters().getVmId(),
                            vdcReturnValue.getFault().getMessage());
                }

                noImagesRemovedYet = false;
            }
        }

        setSucceeded(true);
    }

    @Override
    protected void EndVmCommand() {
        setSucceeded(true);
    }
}

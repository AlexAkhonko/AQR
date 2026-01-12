package org.aqr.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.aqr.entity.Container;
import org.aqr.repository.ContainerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;

@Service
@Transactional
public class ContainerService {

    @Autowired
    private ContainerRepository containerRepository;

//    public List<Container> findByOwnerId(Long ownerId) {
//        return containerRepository.findByOwnerId(ownerId);
//    }

    public Container findById(Long id) {
        return containerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Container not found"));
    }

    public Container save(Container container) {
        return containerRepository.save(container);
    }

//    public Container update(Long id, Container container, Long ownerId) {
//        Container existing = findById(id);
//        if (!existing.getOwner().getId().equals(ownerId)) {
//            //throw new AccessDeniedException("Not owner");
//        }
//        existing.setImage(container.getImage());
//        // parentContainer обновляется через setter
//        return containerRepository.save(existing);
//    }

//    public void delete(Long id, Long ownerId) {
//        Container container = findById(id);
//        if (!container.getOwner().getId().equals(ownerId)) {
//           // throw new AccessDeniedException("Not owner");
//        }
//        containerRepository.delete(container);
//    }

//    public List<Container> findChildrenByParentId(Long parentId, Long ownerId) {
//        Container parent = findById(parentId);
//        if (!parent.getOwner().getId().equals(ownerId)) {
//            throw new AccessDeniedException("Not owner");
//        }
//        return containerRepository.findChildrenByParentId(parentId);
//    }
}
